/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.springcloud.dependency.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.PsiFile;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.maven.DependencyArtifact;
import com.microsoft.intellij.maven.SpringCloudDependencyManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AddAzureDependencyAction extends AzureAnAction {
    public static final String SPRING_CLOUD_GROUP_ID = "org.springframework.cloud";
    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
    private static final String GROUP_ID = "com.microsoft.azure";
    private static final String ARTIFACT_ID = "spring-cloud-starter-azure-spring-cloud-client";

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = event.getData(LangDataKeys.MODULE);
        final Project project = module.getProject();
        final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        final MavenProject mavenProject = projectsManager.findProject(module);
        if (mavenProject == null) {
            PluginUtil.showErrorNotificationProject(project, "Error",
                                                    String.format("Project '%s' is not a maven project.",
                                                                  project.getName()));
            return true;
        }

        DefaultLoader.getIdeHelper().runInBackground(project, "Deleting Docker Host", false, true, "Update Azure Spring Cloud dependencies", () -> {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setText("Syncing maven project " + project.getName());
            if (projectsManager.hasScheduledProjects()) {
                projectsManager.forceUpdateProjects(Collections.singletonList(mavenProject)).get();
            }
            try {
                progressIndicator.setText("Check existing dependencies");
                final String evaluateEffectivePom = MavenUtils.evaluateEffectivePom(project, mavenProject);
                ProgressManager.checkCanceled();
                if (StringUtils.isEmpty(evaluateEffectivePom)) {
                    PluginUtil.showErrorNotificationProject(project, "Error", "Failed to evaluate effective pom.");
                    return;
                }
                final String springBootVer = getMavenLibraryVersion(mavenProject, SPRING_BOOT_GROUP_ID, "spring-boot-autoconfigure");
                if (StringUtils.isEmpty(springBootVer)) {
                    throw new AzureExecutionException(String.format("Module %s is not a spring-boot application.", module.getName()));
                }
                progressIndicator.setText("Get latest versions ...");
                SpringCloudDependencyManager manager = new SpringCloudDependencyManager(evaluateEffectivePom);
                Map<String, DependencyArtifact> versionMaps = manager.getDependencyVersions();
                List<DependencyArtifact> dep = new ArrayList<>();
                dep.add(getDependencyArtifact(GROUP_ID, ARTIFACT_ID, versionMaps));
                dep.add(getDependencyArtifact(SPRING_BOOT_GROUP_ID, "spring-boot-starter-actuator", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-config-client", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID,
                                              "spring-cloud-starter-netflix-eureka-client",
                                              versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-zipkin", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-sleuth", versionMaps));
                ProgressManager.checkCanceled();
                List<DependencyArtifact> versionChanges = SpringCloudDependencyManager.getCompatibleVersions(dep, springBootVer);
                if (versionChanges.isEmpty()) {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.",
                                                           "No updates are needed.");
                    return;
                }
                progressIndicator.setText("Applying versions ...");
                File pomFile = new File(mavenProject.getFile().getCanonicalPath());
                ProgressManager.checkCanceled();
                Map<String, DependencyArtifact> managementVersions = manager.getDependencyManagementVersions();
                versionChanges.stream().filter(change -> managementVersions.containsKey(change.getKey())).forEach(change -> {
                    String managementVersion = managementVersions.get(change.getKey()).getCurrentVersion();
                    if (StringUtils.equals(change.getCompatibleVersion(), managementVersion)
                            || SpringCloudDependencyManager.isCompatibleVersion(managementVersion, springBootVer)) {
                        change.setCompatibleVersion("");
                        change.setManagementVersion(managementVersion);
                    }
                });
                if (!manager.update(pomFile, versionChanges)) {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.",
                                                           "No updates are needed.");
                    return;
                }

                final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(pomFile);
                RefreshQueue.getInstance().refresh(true, false, null, new VirtualFile[]{vf});
                ApplicationManager.getApplication().invokeLater(() -> {
                    FileEditorManager.getInstance(project).closeFile(vf);
                    FileEditorManager.getInstance(project).openFile(vf, true, true);
                    if (versionChanges.stream().anyMatch(t -> StringUtils.isNotEmpty(t.getCurrentVersion()))) {
                        PluginUtil.showInfoNotificationProject(project,
                                                               "Azure Spring Cloud dependencies are updated successfully.",
                                                               summaryVersionChanges(versionChanges));
                    } else {
                        PluginUtil.showInfoNotificationProject(project,
                                                               "Azure Spring Cloud dependencies are added to your project successfully.",
                                                               summaryVersionChanges(versionChanges));
                    }
                });
            } catch (DocumentException | IOException | AzureExecutionException | MavenProcessCanceledException e) {
                PluginUtil.showErrorNotification("Error",
                                                 "Failed to update Azure Spring Cloud dependencies due to error: "
                                                         + e.getMessage());
            }
        });

        return false;
    }

    public void update(@NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final Module module = event.getData(LangDataKeys.MODULE);
        boolean isMaven = module != null && MavenRunTaskUtil.isMavenProject(module.getProject());
        if (isMaven && StringUtils.equals(event.getPlace(), "EditorPopup")) {
            presentation.setEnabledAndVisible(isEditingMavenPomXml(module, event));
        } else {
            presentation.setEnabledAndVisible(isMaven && ModuleTypeId.JAVA_MODULE.equals(module.getModuleTypeName()));
        }
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SPRING_CLOUD;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.ADD_DEPENDENCY_SPRING_CLOUD_APP;
    }

    protected static boolean isEditingMavenPomXml(Module module, AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return false;
        }
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        boolean isMaven = module != null && MavenRunTaskUtil.isMavenProject(module.getProject());
        if (!isMaven) {
            return false;
        }
        final MavenProjectsManager manager = MavenProjectsManager.getInstance(module.getProject());
        final MavenProject mavenProject = manager.findProject(module);

        return editor.getEditorKind() == EditorKind.MAIN_EDITOR && psiFile != null && mavenProject != null &&
                psiFile.getVirtualFile().equals(mavenProject.getFile());
    }

    private static String summaryVersionChanges(List<DependencyArtifact> changes) {
        StringBuilder builder = new StringBuilder();
        for (DependencyArtifact change : changes) {
            boolean isUpdate = StringUtils.isNotEmpty(change.getCurrentVersion());
            builder.append(String.format("%s dependency: Group: %s, Artifact: %s, Version: %s%s \n",
                                         isUpdate ? "Update" : "Add ",
                                         change.getGroupId(),
                                         change.getArtifactId(),
                                         isUpdate ? (change.getCurrentVersion() + " -> ") : "",
                                         StringUtils.isNotEmpty(change.getCompatibleVersion()) ? change.getCompatibleVersion() :
                                         change.getManagementVersion()));
        }
        return builder.toString();
    }

    private static DependencyArtifact getDependencyArtifact(String groupId, String artifactId, Map<String, DependencyArtifact> versionMap) {
        return versionMap.computeIfAbsent(groupId + ":" + artifactId, key -> new DependencyArtifact(groupId, artifactId));
    }

    private static String getMavenLibraryVersion(MavenProject project, String groupId, String artifactId) {
        MavenArtifact lib = project.getDependencies().stream().filter(dep -> !StringUtils.equals(dep.getScope(), "test")
                && isMatch(dep, groupId, artifactId)).findFirst().orElse(null);
        return lib != null ? lib.getVersion() : null;
    }

    private static boolean isMatch(MavenArtifact lib, String groupId, String artifactId) {
        return StringUtils.equals(lib.getArtifactId(), artifactId) && StringUtils.equals(lib.getGroupId(), groupId);
    }
}
