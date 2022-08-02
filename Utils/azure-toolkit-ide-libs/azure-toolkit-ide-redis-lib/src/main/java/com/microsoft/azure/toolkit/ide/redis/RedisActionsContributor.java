/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.redis;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.redis.RedisCache;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class RedisActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.redis.service";
    public static final String REDIS_ACTIONS = "actions.redis.instance";
    public static final Action.Id<AzResource<?, ?, ?>> OPEN_EXPLORER = Action.Id.of("redis.open_explorer");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_REDIS = Action.Id.of("group.create_redis");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder showExplorerView = new ActionView.Builder("Open Redis Explorer")
            .title(s -> Optional.ofNullable(s).map(r -> description("redis.open_redis_explorer.redis", ((AzResourceBase) r).getName())).orElse(null))
            .enabled(s -> s instanceof RedisCache && ((RedisCache) s).getFormalStatus().isRunning());
        final Action<AzResource<?, ?, ?>> action = new Action<>(OPEN_EXPLORER, showExplorerView);
        action.setShortcuts(am.getIDEDefaultShortcuts().view());
        am.registerAction(OPEN_EXPLORER, action);

        final ActionView.Builder createRedisView = new ActionView.Builder("Redis Cache")
            .title(s -> Optional.ofNullable(s).map(r -> description("redis.create_redis.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_REDIS, new Action<>(GROUP_CREATE_REDIS, createRedisView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup redisActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            RedisActionsContributor.OPEN_EXPLORER,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(REDIS_ACTIONS, redisActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_REDIS);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
