/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CreateSpringCloudAppAction {
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
            "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring Cloud App";

    public static void createApp(@Nonnull SpringCloudCluster cluster, @Nullable Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(cluster, project);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                createApp(config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "springcloud|app.create", params = "config.getAppName()", type = AzureOperation.Type.ACTION)
    private static void createApp(SpringCloudAppConfig config) {
        AzureTaskManager.getInstance().runInBackground(AzureOperationBundle.title("springcloud|app.create", config.getAppName()), () -> {
            final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(config);
            final SpringCloudDeployment deployment = task.execute();
            if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                AzureMessager.getMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE);
            }
        });
    }
}
