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

package com.microsoft.azuretools.ijidea.utility;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker;
import com.intellij.openapi.externalSystem.autoimport.ProjectNotificationAware;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.psi.PsiFile;
import com.microsoft.intellij.maven.DependencyArtifact;
import com.microsoft.intellij.maven.MavenDependencyManager;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class AzureMavenDependencyActionBase extends AzureAnAction {
    public void update(@NotNull AnActionEvent event) {
        final Presentation presentation = event.getPresentation();
        final Module module = event.getData(LangDataKeys.MODULE);
        boolean isMaven = module != null && MavenUtils.isMavenProject(module.getProject());
        if (isMaven && StringUtils.equals(event.getPlace(), "EditorPopup")) {
            presentation.setEnabledAndVisible(isEditingMavenPomXml(module, event));
        } else {
            presentation.setEnabledAndVisible(isMaven && ModuleTypeId.JAVA_MODULE.equals(module.getModuleTypeName()));
        }
    }

    protected static boolean syncMavenDependency(MavenProjectsManager projectsManager, Project project, MavenProject mavenProject) {
        final SettableFuture<Boolean> isDirty = SettableFuture.create();

        ApplicationManager.getApplication().invokeAndWait(() -> {
            ProjectNotificationAware notificationAware = ProjectNotificationAware.getInstance(project);
            isDirty.set(notificationAware.isNotificationVisible());
            if (notificationAware.isNotificationVisible()) {
                ExternalSystemProjectTracker projectTracker = ExternalSystemProjectTracker.getInstance(project);
                projectTracker.scheduleProjectRefresh();
            }
        });
        try {
            if (isDirty.get().booleanValue()) {
                projectsManager.forceUpdateProjects(Collections.singletonList(mavenProject)).get();
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            PluginUtil.showErrorNotification("Error",
                                             "Failed to update project due to error: "
                                                     + e.getMessage());

        }
        return false;
    }

    protected static boolean isEditingMavenPomXml(Module module, AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return false;
        }
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        boolean isMaven = module != null && MavenUtils.isMavenProject(module.getProject());
        if (!isMaven) {
            return false;
        }
        final MavenProjectsManager manager = MavenProjectsManager.getInstance(module.getProject());
        final MavenProject mavenProject = manager.findProject(module);

        return editor.getEditorKind() == EditorKind.MAIN_EDITOR && psiFile != null && mavenProject != null &&
                psiFile.getVirtualFile().equals(mavenProject.getFile());
    }

    protected static DependencyArtifact getDependencyArtifact(String groupId, String artifactId, Map<String, DependencyArtifact> versionMap) {
        return versionMap.computeIfAbsent(groupId + ":" + artifactId, key -> new DependencyArtifact(groupId, artifactId));
    }

    protected static String summaryVersionChanges(List<DependencyArtifact> changes) {
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

    protected static void noticeUserVersionChanges(Project project, File pomFile, List<DependencyArtifact> versionChanges) {
        final VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(pomFile);
        RefreshQueue.getInstance().refresh(true, false, null, new VirtualFile[]{vf});
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).closeFile(vf);
            FileEditorManager.getInstance(project).openFile(vf, true, true);
            if (versionChanges.stream().anyMatch(t -> StringUtils.isNotEmpty(t.getCurrentVersion()))) {
                PluginUtil.showInfoNotificationProject(project,
                                                       "Dependencies are updated successfully.",
                                                       summaryVersionChanges(versionChanges));
            } else {
                PluginUtil.showInfoNotificationProject(project,
                                                       "Dependencies are added to your project successfully.",
                                                       summaryVersionChanges(versionChanges));
            }
        });
    }

    protected static boolean applyVersionChanges(MavenDependencyManager dependencyManager,
                                                 File pomFile,
                                                 Map<String, DependencyArtifact> managerDependencyVersionsMaps,
                                                 List<DependencyArtifact> versionChanges) throws IOException,
                                                                                                 DocumentException {
        versionChanges.stream().filter(change -> managerDependencyVersionsMaps.containsKey(change.getKey())).forEach(change -> {
            String managementVersion = managerDependencyVersionsMaps.get(change.getKey()).getCurrentVersion();
            if (StringUtils.equals(change.getCompatibleVersion(), managementVersion)) {
                change.setCompatibleVersion("");
                change.setManagementVersion(managementVersion);
            }
        });
        return dependencyManager.update(pomFile, versionChanges);
    }

    protected static String getMavenLibraryVersion(MavenProject project, String groupId, String artifactId) {
        MavenArtifact lib = project.getDependencies().stream().filter(dep -> !StringUtils.equals(dep.getScope(), "test")
                && isMatch(dep, groupId, artifactId)).findFirst().orElse(null);
        return lib != null ? lib.getVersion() : null;
    }

    protected static boolean isMatch(MavenArtifact lib, String groupId, String artifactId) {
        return StringUtils.equals(lib.getArtifactId(), artifactId) && StringUtils.equals(lib.getGroupId(), groupId);
    }
}
