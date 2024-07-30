/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.redis.RedisActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager.AzureResourceFileType;
import com.microsoft.azure.toolkit.intellij.redis.creation.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.intellij.redis.explorer.RedisCacheExplorerProvider;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import com.microsoft.azure.toolkit.redis.model.RedisConfig;

import javax.swing.*;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijRedisActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureRedis;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateRedisCacheAction.create(e.getProject(), null);
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.RedisCache.MODULE);
        final String name = RedisCacheExplorerProvider.TYPE;
        final AzureResourceFileType type = new AzureResourceFileType(name, icon);
        final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> type);
        am.<AzResource, AnActionEvent>registerHandler(RedisActionsContributor.OPEN_EXPLORER, (r, e) -> r instanceof RedisCache,
            (r, e) -> manager.showEditor(r, Objects.requireNonNull(e.getProject())));

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateServerHandler = (r, e) -> {
            final RedisConfig config = new RedisConfig();
            config.setSubscription(r.getSubscription());
            config.setRegion(r.getRegion());
            config.setResourceGroup(r);
            CreateRedisCacheAction.create(e.getProject(), config);
        };
        am.registerHandler(RedisActionsContributor.GROUP_CREATE_REDIS, (r, e) -> true, groupCreateServerHandler);
    }

    @Override
    public int getOrder() {
        return RedisActionsContributor.INITIALIZE_ORDER + 1;
    }
}
