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
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.runner.container.AzureDockerSupportConfigurationType;
import com.microsoft.intellij.runner.container.pushimage.PushImageRunConfiguration;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

@Name("Push Image")
public class PushToContainerRegistryAction extends NodeActionListener {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DIALOG_TITLE = "Push Image";
    private final ContainerRegistryNode currentNode;
    private final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();

    public PushToContainerRegistryAction(ContainerRegistryNode node) {
        super();
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
                AzureTaskRunner.getInstance().runLater(() -> runConfiguration(project));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void runConfiguration(@NotNull Project project) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getPushImageRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s", factory.getName(), project.getName())
        );
        Observable.fromCallable(() -> {
            Registry registry = ContainerRegistryMvpModel.getInstance().getContainerRegistry(currentNode
                    .getSubscriptionId(), currentNode.getResourceId());
            return ContainerRegistryMvpModel.getInstance().createImageSettingWithRegistry(registry);
        })
                .subscribeOn(SchedulerProviderFactory.getInstance().getSchedulerProvider().io())
                .subscribe(
                        ret -> {
                            if (settings != null) {
                                PushImageRunConfiguration conf = (PushImageRunConfiguration) settings
                                        .getConfiguration();
                                PrivateRegistryImageSetting imageSetting = conf.getPrivateRegistryImageSetting();
                                imageSetting.setServerUrl(ret.getServerUrl());
                                imageSetting.setUsername(ret.getUsername());
                                imageSetting.setPassword(ret.getPassword());
                                AzureTaskRunner.getInstance().runLater(() -> openRunDialog(project, settings));
                                return;
                            }
                            AzureTaskRunner.getInstance().runLater(() -> openRunDialog(project, ret));
                        },
                        err -> {
                            err.printStackTrace();
                            Notification notification = new Notification(NOTIFICATION_GROUP_ID, DIALOG_TITLE,
                                    err.getMessage(), NotificationType.ERROR);
                            Notifications.Bus.notify(notification);
                        });
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void openRunDialog(Project project, RunnerAndConfigurationSettings settings) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    private void openRunDialog(Project project, PrivateRegistryImageSetting imageSetting) {
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getPushImageRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.createConfiguration(
                String.format("%s: %s", factory.getName(), project.getName()), factory);
        PushImageRunConfiguration conf = (PushImageRunConfiguration) settings.getConfiguration();
        conf.setPrivateRegistryImageSetting(imageSetting);

        openRunDialog(project, settings);
    }
}
