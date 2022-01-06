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
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.redis.RedisCache;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class RedisActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.redis.service";
    public static final String REDIS_ACTIONS = "actions.redis.instance";
    public static final Action.Id<IAzureBaseResource<?, ?>> OPEN_EXPLORER = Action.Id.of("action.redis.open_explorer");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder showExplorerView = new ActionView.Builder("Open Redis Explorer")
            .title(s -> Optional.ofNullable(s).map(r -> title("redis.open_redis_explorer.redis", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof RedisCache);
        am.registerAction(OPEN_EXPLORER, new Action<>(showExplorerView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.SERVICE_REFRESH,
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup redisActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                RedisActionsContributor.OPEN_EXPLORER,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.REFRESH
        );
        am.registerGroup(REDIS_ACTIONS, redisActionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
