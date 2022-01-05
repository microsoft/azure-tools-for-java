/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.redis;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class RedisExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Redis Caches";
    private static final String ICON = "/icons/rediscache.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzureActionManager am = AzureActionManager.getInstance();
        final IAzureMessager messager = AzureMessager.getDefaultMessager();

        final AzureRedis service = az(AzureRedis.class);
        final Function<AzureRedis, List<RedisCache>> caches = asc -> asc.list().stream().flatMap(m -> m.caches().list().stream())
            .collect(Collectors.toList());
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
            .actions(RedisActionsContributor.SERVICE_ACTIONS)
            .addChildren(caches, (redis, serviceNode) -> new Node<>(redis)
                .view(new AzureResourceLabelView<>(redis))
                .actions(RedisActionsContributor.REDIS_ACTIONS));
    }
}
