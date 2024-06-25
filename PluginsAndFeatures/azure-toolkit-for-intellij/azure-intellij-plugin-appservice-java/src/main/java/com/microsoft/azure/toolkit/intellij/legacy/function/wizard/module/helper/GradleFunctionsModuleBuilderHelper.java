/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.wizard.module.helper;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.PathKt;
import com.microsoft.azure.toolkit.intellij.legacy.function.wizard.AzureFunctionsConstants;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.open.GradleProjectImportUtil;
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.nio.file.Path;

public class GradleFunctionsModuleBuilderHelper {

    public static void setupGradleModule(final WizardContext wizardContext, @NotNull ModifiableRootModel rootModel,
                                         final String contentEntryPath) throws ConfigurationException {
        // refers org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder.setupRootModel
        final String parentPath = wizardContext.getUserData(AzureFunctionsConstants.PARENT_PATH);
        final String artifactId = wizardContext.getUserData(AzureFunctionsConstants.WIZARD_ARTIFACTID_KEY);
        final Project project = rootModel.getProject();
        final File contentRootDir = new File(contentEntryPath);
        FileUtilRt.createDirectory(contentRootDir);
        final LocalFileSystem fileSystem = LocalFileSystem.getInstance();
        final VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);
        rootModel.addContentEntry(modelContentRootDir);

        final Module module = rootModel.getModule();
        final Path rootProjectPath = wizardContext.isCreatingNewProject() ? modelContentRootDir.toNioPath() : Path.of(parentPath);
        AbstractGradleModuleBuilder.setupGradleSettingsFile(rootProjectPath, modelContentRootDir, project.getName(), artifactId,
                wizardContext.isCreatingNewProject(), false);
        AzureTaskManager.getInstance().runLater(() -> {
            if (wizardContext.isCreatingNewProject()) {
                linkGradleProject(project);
            } else {
                reloadGradleProject(project, rootProjectPath);
            }
        });
    }

    private static void linkGradleProject(@NotNull Project project) {
        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized(() -> {
            GradleProjectImportUtil.linkAndRefreshGradleProject(project.getBasePath(), project);
        });
    }

    private static void reloadGradleProject(@NotNull Project project, @NotNull Path rootProjectPath) {
        ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized(() -> {
            final ImportSpecBuilder importSpec = new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID);
            importSpec.createDirectoriesForEmptyContentRoots();
            ExternalSystemUtil.refreshProject(PathKt.getSystemIndependentPath(rootProjectPath), importSpec);
        });
    }
}
