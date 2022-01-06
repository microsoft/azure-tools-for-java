/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.redis.RedisActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.AzureResourceFileType;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.redis.connection.RedisResourceDefinition;
import com.microsoft.azure.toolkit.intellij.redis.creation.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.intellij.redis.explorer.RedisCacheExplorerProvider;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;

import javax.swing.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijRedisActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureRedis;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateRedisCacheAction.createRedisCache((e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        am.<IAzureBaseResource<?, ?>, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof RedisCache,
            (r, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                dialog.setResource(new AzureServiceResource<>(((RedisCache) r), RedisResourceDefinition.INSTANCE));
                dialog.show();
            }));
        final Icon icon = AzureIcons.getIcon("/icons/rediscache.svg");
        final String name = RedisCacheExplorerProvider.TYPE;
        final AzureResourceFileType type = new AzureResourceFileType(name, icon);
        final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> type);
        am.<IAzureBaseResource<?, ?>, AnActionEvent>registerHandler(RedisActionsContributor.OPEN_EXPLORER, (r, e) -> r instanceof RedisCache,
            (r, e) -> manager.showEditor(r, Objects.requireNonNull(e.getProject())));
    }

    @Override
    public int getOrder() {
        return RedisActionsContributor.INITIALIZE_ORDER + 1;
    }
}
