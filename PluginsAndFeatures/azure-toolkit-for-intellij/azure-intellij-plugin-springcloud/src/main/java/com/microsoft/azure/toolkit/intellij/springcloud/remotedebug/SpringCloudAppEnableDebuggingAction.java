/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceSelectionDialog;
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
    private static final String FAILED_TITLE = "Failed to %s remote debugging";
    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app %s.";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app %s.";
    private static final String CONFIRM_MESSAGE = "Are you sure to %s remote debugging for %s?";
    private static final String CONFIRM_DIALOG_TITLE = "%s Remote Debugging";
    private static final String SUCCESS_MESSAGE = "Remote debugging is %sd for app %s successfully";

    @AzureOperation(name = "springcloud.enable_remote_debugging.app", params = {"app.getName()"}, type = AzureOperation.Type.ACTION)
    public static void enableRemoteDebugging(@Nonnull SpringCloudApp app, @Nullable Project project) {
        toggleDebuggingAction(true, app, project);
    }

    public static void disableRemoteDebugging(@Nonnull SpringCloudApp app, @Nullable Project project) {
        toggleDebuggingAction(false, app, project);
    }

    private static void toggleDebuggingAction(boolean isEnabled, @Nonnull SpringCloudApp app, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final String action = isEnabled ? "enable" : "disable";
        final AzureString title = isEnabled ? OperationBundle.description("springcloud.enable_remote_debugging.app", app.getName()) :
                OperationBundle.description("springcloud.disable_remote_debugging.app", app.getName());
        final boolean userInput = AzureMessager.getMessager().confirm(String.format(CONFIRM_MESSAGE, action, app.getName()),
                String.format(CONFIRM_DIALOG_TITLE, Character.toUpperCase(action.charAt(0)) + action.substring(1)));
        if (!userInput) {
            return;
        }
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final SpringCloudDeployment deployment = app.getActiveDeployment();
                if (deployment == null || !deployment.exists()) {
                    messager.error(NO_ACTIVE_DEPLOYMENT, String.format(FAILED_TITLE, action));
                    return;
                }
                if (isEnabled) {
                    app.enableRemoteDebugging(SpringCloudAppInstanceDebuggingAction.getDefaultPort());
                    messager.success(String.format(SUCCESS_MESSAGE, action, app.getName()), null, generateDebugAction(app, project), generateLearnMoreAction());
                } else {
                    app.disableRemoteDebugging();
                    messager.success(String.format(SUCCESS_MESSAGE, action, app.getName()));
                }
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, String.format(FAILED_TITLE, action));
            } finally {
                app.refresh();
            }
        }));
    }

    private static Action<?> generateDebugAction(@Nonnull SpringCloudApp app, @Nullable Project project) {
        final Action<SpringCloudAppInstance> remoteDebuggingAction = AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.REMOTE_DEBUGGING);
        return new Action<>(Action.Id.of("springcloud.remote_debug_dialog"), new ActionView.Builder("Debug")) {
            @Override
            public void handle(Object source, Object e) {
                final SpringCloudDeployment deployment = app.getActiveDeployment();
                final List<SpringCloudAppInstance> instances = deployment.getInstances();
                if (CollectionUtils.isEmpty(instances)) {
                    AzureMessager.getMessager().error(String.format(NO_AVAILABLE_INSTANCES, app.getName()));
                    return;
                }
                AzureTaskManager.getInstance().runLater(() -> {
                    final SpringCloudAppInstanceSelectionDialog dialog = new SpringCloudAppInstanceSelectionDialog(project, instances);
                    if (dialog.showAndGet()) {
                        final SpringCloudAppInstance target = dialog.getInstance();
                        remoteDebuggingAction.handle(target, e);
                    }
                });
            }
        };
    }

    private static Action<?> generateLearnMoreAction() {
        return new Action<>(Action.Id.of("springcloud.learn_more_dialog"), new ActionView.Builder("Learn More")) {
            @Override
            public void handle(Object source, Object e) {
                final String remoteDebuggingDocs = "https://aka.ms/asa-remotedebug";
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(remoteDebuggingDocs);
            }
        };
    }
}
