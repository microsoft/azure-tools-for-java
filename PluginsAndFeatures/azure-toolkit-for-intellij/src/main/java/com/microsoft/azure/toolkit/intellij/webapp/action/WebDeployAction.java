/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.webapp.runner.WebAppConfigurationType;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.intellij.actions.AzureSignInAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebDeployAction extends AzureAnAction {

    private final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
        if (module == null) {
            return true;
        }
        AzureSignInAction.requireSignedIn(module.getProject(), () -> runConfiguration(module));
        return false;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.WEBAPP;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.DEPLOY_WEBAPP;
    }

    @SuppressWarnings("deprecation")
    @AzureOperation(name = "webapp.deploy_module", params = {"module.getName()"}, type = AzureOperation.Type.ACTION)
    private void runConfiguration(Module module) {
        Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getWebAppConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()),
                    factory);
        }
        if (RunDialog.editConfiguration(project, settings, message("webapp.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
