/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.RunConfigurationUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.AzureFunctionSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localrun.FunctionRunConfigurationFactory;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class RunFunctionAction extends AnAction {

    private final AzureFunctionSupportConfigurationType configType = AzureFunctionSupportConfigurationType.getInstance();

    @Override
    public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
        final Module module = LangDataKeys.MODULE.getData(anActionEvent.getDataContext());
        AzureTaskManager.getInstance().runLater(() -> runConfiguration(module));
    }

    @Override
    @ExceptionNotification
    public void update(AnActionEvent e) {
        final boolean onModule = Objects.nonNull(LangDataKeys.MODULE.getData(e.getDataContext()));
        e.getPresentation().setEnabledAndVisible(onModule && FunctionUtils.isFunctionProject(e.getProject()));
    }

    @AzureOperation(name = "user/function.run_app.module", params = {"module.getName()"})
    private void runConfiguration(Module module) {
        // todo: investigate when will module be null
        if (module == null) {
            return;
        }
        final Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = new FunctionRunConfigurationFactory(configType);
        final RunnerAndConfigurationSettings settings = RunConfigurationUtils.getOrCreateRunConfigurationSettings(module, manager, factory);
        if (settings.getConfiguration() instanceof FunctionRunConfiguration) {
            ((FunctionRunConfiguration) settings.getConfiguration()).saveModule(module);
        }
        if (RunDialog.editConfiguration(project, settings, message("function.run.configuration.title"), DefaultRunExecutor.getRunExecutorInstance())) {
            final List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
