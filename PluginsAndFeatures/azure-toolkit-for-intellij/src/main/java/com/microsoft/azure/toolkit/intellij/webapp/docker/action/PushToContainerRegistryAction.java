/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.action;

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
import com.microsoft.azure.toolkit.intellij.webapp.docker.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage.PushImageRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.container.ContainerRegistryMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

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
        AzureSignInAction.requireSignedIn(project, () -> runConfiguration(project));
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
                                AzureTaskManager.getInstance().runLater(() -> openRunDialog(project, settings));
                                return;
                            }
                            AzureTaskManager.getInstance().runLater(() -> openRunDialog(project, ret));
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
