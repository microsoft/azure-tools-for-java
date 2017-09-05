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

package com.microsoft.intellij.serviceexplorer.azure.container;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunDialog;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryPassword;
import com.microsoft.azure.management.containerregistry.implementation.RegistryListCredentials;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.runner.container.AzureDockerSupportConfigurationType;
import com.microsoft.intellij.runner.container.pushimage.PushImageRunConfiguration;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;

import java.util.ArrayList;
import java.util.List;

@Name("Push Image")
public class PushToContainerRegistryAction extends NodeActionListener {
    private static final String DIALOG_TITLE = "Push Image";
    private final ContainerRegistryNode currentNode;
    private final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();

    public PushToContainerRegistryAction(ContainerRegistryNode node) {
        super(node);
        this.currentNode = node;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        Project project = (Project) nodeActionEvent.getAction().getNode().getProject();
        if (project == null) {
            return;
        }
        try {
            if (AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                ApplicationManager.getApplication().invokeLater(() -> runConfiguration(project));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void runConfiguration(Project project) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getPushImageRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s", factory.getName(), currentNode.getName())
        );
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s", factory.getName(), currentNode.getName()), factory);
            // read configuration for the ACR node.
            // TODO: currently it's blocking, later use a presenter to do this.
            PushImageRunConfiguration conf = (PushImageRunConfiguration) settings.getConfiguration();
            Registry registry = null;
            try {
                registry = ContainerRegistryMvpModel.getInstance().getContainerRegistry(currentNode.getSubscriptionId
                        (), currentNode.getResourceId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (registry !=null && registry.adminUserEnabled()) {
                RegistryListCredentials credentials = registry.listCredentials();
                List<RegistryPassword> passwords = credentials.passwords();
                if (passwords != null && passwords.size() > 0) {
                    PrivateRegistryImageSetting imagesetting = new PrivateRegistryImageSetting(registry
                            .loginServerUrl(), credentials.username(), passwords.get
                            (0).value(), null, null);
                    conf.setPrivateRegistryImageSetting(imagesetting);
                }
            }
        }
        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
