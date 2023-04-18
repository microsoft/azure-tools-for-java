/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerapps.action;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerapps.AzureContainerAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class DeployImageToAzureContainerAppAction {
    @AzureOperation(name = "user/containerapps.open_update_image_dialog.app", params = {"app.getName()"})
    public static void deployImageToAzureContainerApps(@Nullable final VirtualFile file, @Nonnull final AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(() -> runConfiguration(e.getProject(), file));
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private static void runConfiguration(final Project project, @Nullable VirtualFile dockerFile) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final AzureContainerAppConfigurationType configType = AzureContainerAppConfigurationType.getInstance();
        final ConfigurationFactory factory = AzureContainerAppConfigurationType.getInstance().getDeployImageRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(String.format("%s: %s", factory.getName(), project.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(String.format("%s: %s", factory.getName(), project.getName()), factory);
        }
        final RunConfiguration configuration = settings.getConfiguration();
        if (configuration instanceof DockerHostRunConfiguration) {
            Optional.ofNullable(dockerFile).map(DockerImage::new).ifPresent(image -> ((DockerHostRunConfiguration) configuration).setDockerImage(image));
        }
        if (RunDialog.editConfiguration(project, settings, "Deploy Image to Azure Container Apps", DefaultRunExecutor.getRunExecutorInstance())) {
            manager.addConfiguration(settings, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
