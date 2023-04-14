/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RunOnDockerHostAction extends AzureAnAction {

    private static final String DIALOG_TITLE = "Run on Docker Host";
    private DockerImage dockerImage;
    private final AzureDockerSupportConfigurationType configType;

    public RunOnDockerHostAction() {
        this(null);
    }

    public RunOnDockerHostAction(@Nullable final DockerImage dockerImage) {
        super(DIALOG_TITLE, "Build image and run in local docker host", IntelliJAzureIcons.getIcon("/icons/DockerSupport/Run.svg"));
        this.configType = AzureDockerSupportConfigurationType.getInstance();
        this.dockerImage = dockerImage;
    }

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        Module module = DataKeys.MODULE.getData(event.getDataContext());
        if (module == null) {
            return true;
        }
        AzureTaskManager.getInstance().runLater(() -> runConfiguration(module));
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.WEBAPP;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.DEPLOY_WEBAPP_DOCKERHOST;
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void runConfiguration(Module module) {
        Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getDockerHostRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()),
                    factory);
        }
        final RunConfiguration configuration = settings.getConfiguration();
        if (configuration instanceof DockerHostRunConfiguration) {
            Optional.ofNullable(dockerImage).ifPresent(image -> ((DockerHostRunConfiguration) configuration).setDockerImage(image));
        }
        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(configuration));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
