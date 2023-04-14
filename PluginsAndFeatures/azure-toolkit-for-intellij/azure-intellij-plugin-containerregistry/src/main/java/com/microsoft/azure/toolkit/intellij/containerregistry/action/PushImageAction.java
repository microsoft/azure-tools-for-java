/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.action;

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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.container.Constant;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.AzureDockerSupportConfigurationType;
import com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.PushImageRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PushImageAction extends AzureAnAction {
    private static final String NOTIFICATION_GROUP_ID = "Azure Plugin";
    private static final String DIALOG_TITLE = "Push Image";
    private final AzureDockerSupportConfigurationType configType = AzureDockerSupportConfigurationType.getInstance();

    private DockerImage dockerImage;

    public PushImageAction() {
        this(null);
    }

    public PushImageAction(@Nullable DockerImage dockerImage) {
        super(DIALOG_TITLE, "Build/push local image to Azure Container Registry", IntelliJAzureIcons.getIcon("/icons/DockerSupport/PushImage.svg"));
        this.dockerImage = dockerImage;
    }

    @Override
    @AzureOperation(name = "user/docker.push_image")
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {

        Module module = DataKeys.MODULE.getData(event.getDataContext());
        if (module == null) {
            notifyError(Constant.ERROR_NO_SELECTED_PROJECT);
            return true;
        }
        AzureTaskManager.getInstance().runLater(() -> runConfiguration(module));
        return true;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.ACR;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.ACR_PUSHIMAGE;
    }

    @SuppressWarnings({"deprecation", "Duplicates"})
    private void runConfiguration(Module module) {
        Project project = module.getProject();
        final RunManagerEx manager = RunManagerEx.getInstanceEx(project);
        final ConfigurationFactory factory = configType.getPushImageRunConfigurationFactory();
        RunnerAndConfigurationSettings settings = manager.findConfigurationByName(
                String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()));
        if (settings == null) {
            settings = manager.createConfiguration(
                    String.format("%s: %s:%s", factory.getName(), project.getName(), module.getName()),
                    factory);
        }
        if (settings.getConfiguration() instanceof PushImageRunConfiguration) {
            ((PushImageRunConfiguration) settings.getConfiguration()).setDockerImage(dockerImage);
        }
        if (RunDialog.editConfiguration(project, settings, DIALOG_TITLE, DefaultRunExecutor.getRunExecutorInstance())) {
            List<BeforeRunTask> tasks = new ArrayList<>(manager.getBeforeRunTasks(settings.getConfiguration()));
            manager.addConfiguration(settings, false, tasks, false);
            manager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(project, settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }

    private void notifyError(String msg) {
        Notification notification = new Notification(NOTIFICATION_GROUP_ID, DIALOG_TITLE, msg, NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }
}
