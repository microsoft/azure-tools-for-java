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
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.maven.DependencyArtifact;
import com.microsoft.intellij.maven.SpringCloudDependencyManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddAzureDependencyAction extends AzureAnAction {
    private static final String GROUP_ID = "com.microsoft.azure";
    private static final String ARTIFACT_ID = "spring-cloud-starter-azure-spring-cloud-client";
    public static final String SPRING_CLOUD_GROUP_ID = "org.springframework.cloud";
    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = event.getData(LangDataKeys.MODULE);
        final Project project = module.getProject();
        final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        final MavenProject mavenProject = projectsManager.findProject(module);
        if (mavenProject == null) {
            PluginUtil.showErrorNotificationProject(project, "Error",
                                             String.format("Project '%s' is not a maven project.", project.getName()));
            return true;
        }
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setText("Check existing dependencies");
            try {
                // wait 15 minutes for evaluating effective pom;
                final String evaluateEffectivePom = MavenUtils.evaluateEffectivePom(project, mavenProject, 15 * 60);
                if (StringUtils.isEmpty(evaluateEffectivePom)) {
                    PluginUtil.showErrorNotificationProject(project, "Error", "Failed to evaluate effective pom.");
                    return;
                }
                SpringCloudDependencyManager manager = new SpringCloudDependencyManager(evaluateEffectivePom);

                Map<String, DependencyArtifact> versionMaps = manager.getDependencyVersions();
                ProgressManager.checkCanceled();
                DependencyArtifact springBootArtifact = versionMaps.get(SPRING_BOOT_GROUP_ID + ":spring-boot-autoconfigure");
                if (springBootArtifact == null || StringUtils.isEmpty(springBootArtifact.getCurrentVersion())) {
                    throw new AzureExecutionException(String.format("Module %s is not a spring-boot application.", module.getName()));
                }
                final String springBootVer = springBootArtifact.getCurrentVersion();
                ProgressManager.checkCanceled();

                progressIndicator.setText("Get latest versions ...");
                List<DependencyArtifact> dep = new ArrayList<>();
                dep.add(getDependencyArtifact(GROUP_ID, ARTIFACT_ID, versionMaps));
                dep.add(getDependencyArtifact(SPRING_BOOT_GROUP_ID, "spring-boot-starter-actuator", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-config-client", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-netflix-eureka-client", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-zipkin", versionMaps));
                dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-sleuth", versionMaps));
                ProgressManager.checkCanceled();
                List<DependencyArtifact> versionChanges = manager.getCompatibleVersions(dep, springBootVer);
                if (versionChanges.isEmpty()) {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.",
                            "No updates are needed.");
                    return;
                }
                ProgressManager.checkCanceled();
                progressIndicator.setText("Applying versions ...");
                File pomFile = new File(mavenProject.getFile().getCanonicalPath());
                manager.update(pomFile, versionChanges);

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
                        PluginUtil.showInfoNotificationProject(project, "Azure Spring Cloud dependencies are added to your project successfully.",
                                summaryVersionChanges(versionChanges));
                    }
                });
            } catch (DocumentException | IOException | AzureExecutionException e) {
                PluginUtil.showErrorNotification("Error",
                        "Failed to update Azure Spring Cloud dependencies due to error: " + e.getMessage());
            }
        }, "Update Azure Spring Cloud dependencies", true, project);

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

        return editor.getEditorKind() == EditorKind.MAIN_EDITOR && psiFile != null &&
                psiFile.getVirtualFile().equals(mavenProject.getFile());
    }

    private static String summaryVersionChanges(List<DependencyArtifact> changes) {
        StringBuilder builder = new StringBuilder();
        for (DependencyArtifact change : changes) {
            boolean isUpdate = StringUtils.isNotEmpty(change.getCurrentVersion());
            builder.append(String.format("%s dependency: Group: %s, Artifact: %s, Version: %s%s \n",
                    isUpdate ? "Update" : "Add ", change.getGroupId(), change.getArtifactId(),
                    isUpdate ? (change.getCurrentVersion() + " -> ") : "", change.getCompilableVersion()));
        }
        return builder.toString();
    }

    private static DependencyArtifact getDependencyArtifact(String groupId, String artifactId, Map<String, DependencyArtifact> versionMap) {
        return versionMap.computeIfAbsent(groupId + ":" + artifactId, key -> new DependencyArtifact(groupId, artifactId));
    }
}
