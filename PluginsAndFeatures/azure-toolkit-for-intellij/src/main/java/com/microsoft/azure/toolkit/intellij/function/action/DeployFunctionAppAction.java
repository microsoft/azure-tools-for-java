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
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppService;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azure.toolkit.intellij.function.runner.AzureFunctionSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployConfiguration;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeploymentConfigurationFactory;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

// todo: Remove duplicate codes with Web Deploy Action
@Name("Deploy")
public class DeployFunctionAppAction extends NodeActionListener {

    private final AzureFunctionSupportConfigurationType functionType = AzureFunctionSupportConfigurationType.getInstance();

    private final Project project;
    private final FunctionAppNode functionNode;

    public DeployFunctionAppAction(FunctionAppNode functionNode) {
        super();
        this.functionNode = functionNode;
        this.project = (Project) functionNode.getProject();
    }

    @Override
    protected void actionPerformed(final NodeActionEvent nodeActionEvent) throws AzureCmdException {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings(manager);
        if (RunDialog.editConfiguration(project, settings, message("function.deploy.configuration.title"),
                                        DefaultRunExecutor.getRunExecutorInstance())) {
            final List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    private RunnerAndConfigurationSettings getRunConfigurationSettings(RunManagerEx manager) {
        final ConfigurationFactory factory = new FunctionDeploymentConfigurationFactory(functionType);
        final String runConfigurationName = String.format("%s: %s:%s",
                                                          factory.getName(),
                                                          project.getName(),
                                                          functionNode.getName());
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof FunctionDeployConfiguration) {
            final IFunctionApp functionApp = Azure.az(AzureAppService.class).functionApp(functionNode.getId());
            final FunctionAppConfig config = FunctionAppService.getInstance().getFunctionAppConfigFromExistingFunction(functionApp);
            ((FunctionDeployConfiguration) runConfiguration).saveConfig(config);
        }
        return settings;
    }
}
