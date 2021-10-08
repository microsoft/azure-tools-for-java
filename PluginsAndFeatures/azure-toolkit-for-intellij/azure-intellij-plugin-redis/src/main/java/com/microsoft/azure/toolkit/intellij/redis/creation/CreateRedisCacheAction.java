/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.model.RedisConfig;

public class CreateRedisCacheAction {
    private static final String ERROR_CREATING_REDIS_CACHE = "Error creating Redis cache";

    public static void createRedisCache(Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final RedisCreationDialog dialog = new RedisCreationDialog(project);
            dialog.setOkActionListener(config -> {
                doCreate(config, project);
                dialog.close();
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "redis.create", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
    private static void doCreate(final RedisConfig config, final Project project) {
        final AzureString title = AzureOperationBundle.title("redis.create", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg instanceof Draft) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            Azure.az(AzureRedis.class).subscription(config.getSubscription()).create(config);
        });
    }
}
