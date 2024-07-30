/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.redis.connection.RedisResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.redis.RedisCache;

public class IntellijJavaRedisActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<AzResource, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof RedisCache,
                (r, e) -> AzureTaskManager.getInstance().runLater(() -> {
                    final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                    dialog.setResource(new AzureServiceResource<>(((RedisCache) r), RedisResourceDefinition.INSTANCE));
                    dialog.show();
                }));
    }
}