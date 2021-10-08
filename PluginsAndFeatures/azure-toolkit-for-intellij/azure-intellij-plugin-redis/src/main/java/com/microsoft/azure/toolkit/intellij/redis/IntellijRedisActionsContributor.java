/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.redis.creation.CreateRedisCacheAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.redis.AzureRedis;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijRedisActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureRedis;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateRedisCacheAction.createRedisCache((e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);
    }
}
