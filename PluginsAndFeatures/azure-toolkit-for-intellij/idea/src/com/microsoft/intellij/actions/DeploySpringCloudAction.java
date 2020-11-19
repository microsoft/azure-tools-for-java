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


package com.microsoft.intellij.actions;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.azuretools.ijidea.utility.AzureAnAction;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.runner.springcloud.SpringCloudConfigurationType;
import com.microsoft.intellij.util.PluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeploySpringCloudAction extends AzureAnAction {
    private static final String DEPLOY_SPRING_CLOUD_APP_TITLE = "Deploy Azure Spring Cloud App";
    private static final SpringCloudConfigurationType configType = SpringCloudConfigurationType.getInstance();

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        final Module module = anActionEvent.getData(LangDataKeys.MODULE);
        try {
            if (AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), module.getProject())) {
                ApplicationManager.getApplication().invokeLater(() -> deployConfiguration(module));
            }
        } catch (Exception e) {
            ApplicationManager.getApplication().invokeLater(() ->
                    PluginUtil.displayErrorDialog("Failed to deploy spring cloud", e.getMessage()));
        }
        return true;
    }

    private static void deployConfiguration(Module module) {
        final Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getConfigurationFactories()[0];
        final RunnerAndConfigurationSettings settings = RunConfigurationUtils.getOrCreateRunConfigurationSettings(module, manager, factory);
        if (RunDialog.editConfiguration(project, settings, DEPLOY_SPRING_CLOUD_APP_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            final List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
