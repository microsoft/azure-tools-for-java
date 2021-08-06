/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.action;

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
import com.microsoft.azure.toolkit.intellij.function.runner.AzureFunctionSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeploymentConfigurationFactory;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.actions.RunConfigurationUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class DeployFunctionAction extends AzureAnAction {
    private final AzureFunctionSupportConfigurationType configType = AzureFunctionSupportConfigurationType.getInstance();

    @Override
    @AzureOperation(name = "function.deploy.configuration", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        final Module module = DataKeys.MODULE.getData(anActionEvent.getDataContext());
        if (module == null) {
            return true;
        }
        final Project project = anActionEvent.getProject();
        AzureSignInAction.requireSignedIn(project, () -> runConfiguration(module));
        return false;
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(FunctionUtils.isFunctionProject(event.getProject()));
    }

    private void runConfiguration(Module module) {
        // todo: investigate when will module be null
        if (module == null) {
            return;
        }
        final Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = new FunctionDeploymentConfigurationFactory(configType);
        final RunnerAndConfigurationSettings settings = RunConfigurationUtils.getOrCreateRunConfigurationSettings(module, manager, factory);
        if (RunDialog.editConfiguration(project, settings, message("function.deploy.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
            final List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
