/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.intellij.springcloud.streaminglog.SpringCloudStreamingLogDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SpringCloudAppEnableDebuggingAction {
    private static final String FAILED_TO_ENABLE_REMOTE_DEBUGGING = "Failed to enable remote debugging";
    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String START_REMOTE_DEBUGGING = "Remote Debug";
    private static final String FAILED_TO_START_REMOTE_DEBUGGING = "Failed to start remote debugging";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app.";

    @AzureOperation(name = "springcloud.enable_remote_debugging.app", params = {"app.getName()"}, type = AzureOperation.Type.ACTION)
    public static void enableRemoteDebugging(@Nonnull SpringCloudApp app, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final AzureString title = OperationBundle.description("springcloud.enable_remote_debugging.app", app.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final SpringCloudDeployment deployment = app.getActiveDeployment();
                if (deployment == null || !deployment.exists()) {
                    messager.warning(NO_ACTIVE_DEPLOYMENT, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
                    return;
                }
                app.enableRemoteDebugging(SpringCloudAppInstanceDebuggingAction.getDefaultPort());
                final Action<SpringCloudAppInstance> remoteDebuggingAction = AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.REMOTE_DEBUGGING);
                messager.success(String.format("Enable remote debugging for spring app %s successfully.", app.getName()), "", new Action<>(Action.Id.of("springcloud.remote_debug"), new ActionView.Builder("Enable Remote Debugging")) {
                    @Override
                    public void handle(Object source, Object e) {
                        final SpringCloudDeployment deployment = app.getActiveDeployment();
                        if (deployment == null || !deployment.exists()) {
                            AzureMessager.getMessager().warning(NO_ACTIVE_DEPLOYMENT, FAILED_TO_START_REMOTE_DEBUGGING);
                            return;
                        }
                        final List<SpringCloudAppInstance> instances = deployment.getInstanceResources();
                        if (CollectionUtils.isEmpty(instances)) {
                            AzureMessager.getMessager().warning(NO_AVAILABLE_INSTANCES, FAILED_TO_START_REMOTE_DEBUGGING);
                            return;
                        }
                        AzureTaskManager.getInstance().runLater(() -> {
                            final SpringCloudStreamingLogDialog dialog = new SpringCloudStreamingLogDialog(project, instances);
                            if (dialog.showAndGet()) {
                                final SpringCloudAppInstance target = dialog.getInstance();
                                remoteDebuggingAction.handle(target, e);
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
            } finally {
                app.refresh();
            }
        }));
    }

    public static void disableRemoteDebugging(@Nonnull SpringCloudApp app, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final AzureString title = OperationBundle.description("springcloud.disable_remote_debugging.app", app.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final SpringCloudDeployment deployment = app.getActiveDeployment();
                if (deployment == null || !deployment.exists()) {
                    messager.warning(NO_ACTIVE_DEPLOYMENT, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
                    return;
                }
                app.disableRemoteDebugging();
                messager.success(String.format("Disable remote debugging for spring app %s successfully.", app.getName()));
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
            } finally {
                app.refresh();
            }
        }));
    }
}
