/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Objects;

public class DeploySpringCloudAppAction extends AnAction {
    private static final String DEPLOY_SPRING_CLOUD_APP_TITLE = "Deploy Azure Spring Cloud App";
    private static final SpringCloudDeploymentConfigurationType configType = SpringCloudDeploymentConfigurationType.getInstance();

    @AzureOperation(name = "springcloud.deploy_app", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        if (project != null) {
            AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle(() -> deploy((SpringCloudApp) null, project));
        }
    }

    public static void deploy(@Nullable SpringCloudApp app, @Nonnull Project project) {
        final RunnerAndConfigurationSettings settings = getConfigurationSettings(project);
        final SpringCloudDeploymentConfiguration configuration = ((SpringCloudDeploymentConfiguration) settings.getConfiguration());
        configuration.setApp(app);
        runConfiguration(project, settings, configuration);
    }

    public static void deploy(@Nonnull SpringCloudDeploymentConfiguration configuration, @Nonnull Project project) {
        final RunnerAndConfigurationSettings settings = getConfigurationSettings(project);
        runConfiguration(project, settings, configuration);
    }

    private static void runConfiguration(@Nonnull Project project, RunnerAndConfigurationSettings settings, SpringCloudDeploymentConfiguration configuration) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        AzureTaskManager.getInstance().runLater(() -> {
            if (RunDialog.editConfiguration(project, settings, DEPLOY_SPRING_CLOUD_APP_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
                settings.storeInLocalWorkspace();
                manager.addConfiguration(settings);
                manager.setBeforeRunTasks(configuration, new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration())));
                manager.setSelectedConfiguration(settings);
                ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
            }
        });
    }

    @Nonnull
    private static RunnerAndConfigurationSettings getConfigurationSettings(@Nonnull Project project) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getConfigurationFactories()[0];
        final String configurationName = String.format("%s: %s", factory.getName(), project.getName());
        final RunnerAndConfigurationSettings existed = manager.findConfigurationByName(configurationName);
        return Objects.nonNull(existed) ? existed : manager.createConfiguration(configurationName, factory);
    }
}
