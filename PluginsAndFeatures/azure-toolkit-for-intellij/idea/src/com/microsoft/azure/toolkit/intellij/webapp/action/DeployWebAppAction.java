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

package com.microsoft.azure.toolkit.intellij.webapp.action;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.intellij.runner.webapp.WebAppConfigurationType;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;

import java.util.ArrayList;
import java.util.List;

// todo: Remove duplicate codes with Web Deploy Action
@Name("Deploy")
public class DeployWebAppAction extends NodeActionListener {

    private final WebAppConfigurationType configType = WebAppConfigurationType.getInstance();
    private Project project;
    private WebAppNode webAppNode;

    public DeployWebAppAction(WebAppNode webAppNode) {
        this.webAppNode = webAppNode;
        this.project = (Project) webAppNode.getProject();
    }

    @Override
    protected void actionPerformed(final NodeActionEvent nodeActionEvent) throws AzureCmdException {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final RunnerAndConfigurationSettings settings = getRunConfigurationSettings(manager);
        if (RunDialog.editConfiguration(project, settings, "Deploy to Azure",
                                        DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    private RunnerAndConfigurationSettings getRunConfigurationSettings(RunManagerEx manager) {
        final ConfigurationFactory factory = configType.getWebAppConfigurationFactory();
        final String runConfigurationName = String.format("%s: %s:%s",
                                                          factory.getName(),
                                                          project.getName(),
                                                          webAppNode.getName());
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(runConfigurationName);
        if (settings == null) {
            settings = manager.createConfiguration(runConfigurationName, factory);
        }
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (runConfiguration instanceof WebAppConfiguration) {
            ((WebAppConfiguration) runConfiguration).setWebAppId(webAppNode.getWebAppId());
            ((WebAppConfiguration) runConfiguration).setWebAppName(webAppNode.getWebAppName());
            ((WebAppConfiguration) runConfiguration).setSubscriptionId(webAppNode.getSubscriptionId());
        }
        return settings;
    }
}
