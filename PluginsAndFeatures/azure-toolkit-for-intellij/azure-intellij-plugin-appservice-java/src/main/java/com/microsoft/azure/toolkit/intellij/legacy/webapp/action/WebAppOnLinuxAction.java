/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction;
import com.microsoft.azure.toolkit.intellij.common.auth.AzureLoginHelper;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WebAppOnLinuxAction extends AzureAnAction {

    private static final String DIALOG_TITLE = "Run on Web App for Containers";

    private DockerImage dockerImage;
    private final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();

    public WebAppOnLinuxAction() {
        this(null);
    }

    public WebAppOnLinuxAction(@Nullable DockerImage dockerImage) {
        super(DIALOG_TITLE, "Build/push local image to Azure Web App", IntelliJAzureIcons.getIcon("/icons/DockerSupport/RunOnWebApp.svg"));
        this.dockerImage = dockerImage;
    }

    @Override
    @AzureOperation(name = "user/docker.start_app")
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        Module module = DataKeys.MODULE.getData(event.getDataContext());
        if (module == null) {
            return true;
        }
        AzureLoginHelper.requireSignedIn(module.getProject(), () -> runConfiguration(module));
        return false;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.WEBAPP;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.DEPLOY_WEBAPP_CONTAINER;
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void runConfiguration(Module module) {
        Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getWebAppOnLinuxConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()),
                    factory);
        }
        if (settings.getConfiguration() instanceof WebAppOnLinuxDeployConfiguration) {
            ((WebAppOnLinuxDeployConfiguration) settings.getConfiguration()).setDockerImage(dockerImage);
        }
        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
