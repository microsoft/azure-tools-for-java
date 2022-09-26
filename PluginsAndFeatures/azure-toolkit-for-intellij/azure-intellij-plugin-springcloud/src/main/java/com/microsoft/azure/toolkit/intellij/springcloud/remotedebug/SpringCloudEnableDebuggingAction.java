package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpringCloudEnableDebuggingAction {
    private static final String FAILED_TO_ENABLE_REMOTE_DEBUGGING = "Failed to enable remote debugging";
    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app.";
    private static final int DEFAULT_PORT = 5005;


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
                app.enableDebugging(DEFAULT_PORT);
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
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
                app.enableDebugging(DEFAULT_PORT);
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, FAILED_TO_ENABLE_REMOTE_DEBUGGING);
            }
        }));
    }
}
