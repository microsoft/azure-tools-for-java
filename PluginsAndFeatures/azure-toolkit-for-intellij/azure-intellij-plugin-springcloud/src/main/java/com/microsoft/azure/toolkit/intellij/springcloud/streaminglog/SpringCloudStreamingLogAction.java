/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceSelectionDialog;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class SpringCloudStreamingLogAction {

    private static final String FAILED_TO_START_LOG_STREAMING = "Failed to start log streaming";

    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app.";
    private static final String FAILED_TO_LIST_INSTANCES = "Failed to list Spring app instances.";
    private static final String FAILED_TO_LIST_INSTANCES_WITH_MESSAGE = "Failed to list Spring app instances: %s";

    public static void startLogStreaming(@Nonnull SpringCloudApp app, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        final AzureString title = OperationBundle.description("springcloud.open_log_stream.instance", app.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final SpringCloudDeployment deployment = app.getActiveDeployment();
                if (deployment == null || !deployment.exists()) {
                    messager.warning(NO_ACTIVE_DEPLOYMENT, FAILED_TO_START_LOG_STREAMING);
                    return;
                }
                final List<SpringCloudAppInstance> instances = deployment.getInstances();
                if (CollectionUtils.isEmpty(instances)) {
                    messager.warning(NO_AVAILABLE_INSTANCES, FAILED_TO_START_LOG_STREAMING);
                } else {
                    showLogStreamingDialog(instances, app, project);
                }
            } catch (final Exception e) {
                final String errorMessage = StringUtils.isEmpty(e.getMessage()) ?
                        FAILED_TO_LIST_INSTANCES : String.format(FAILED_TO_LIST_INSTANCES_WITH_MESSAGE, e.getMessage());
                messager.error(errorMessage, FAILED_TO_START_LOG_STREAMING);
            }
        }));
    }

    private static void showLogStreamingDialog(List<SpringCloudAppInstance> instances, SpringCloudApp app, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppInstanceSelectionDialog dialog = new SpringCloudAppInstanceSelectionDialog(project, instances);
            if (dialog.showAndGet()) {
                final SpringCloudAppInstance target = dialog.getInstance();
                SpringCloudStreamingLogManager.getInstance().showStreamingLog(project, app, target.getName());
            }
        });
    }
}
