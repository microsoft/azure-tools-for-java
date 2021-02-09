/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class RedisCacheModule extends AzureRefreshableNode {
    private static final String REDIS_SERVICE_MODULE_ID = RedisCacheModule.class.getName();
    private static final String ICON_PATH = "RedisCache.png";
    private static final String BASE_MODULE_NAME = "Redis Caches";
    private final RedisCacheModulePresenter<RedisCacheModule> redisCachePresenter;

    public static final String MODULE_NAME = "Redis Cache";

    /**
     * Create the node containing all the Redis Cache resources.
     *
     * @param parent
     *            The parent node of this node
     */
    public RedisCacheModule(Node parent) {
        super(REDIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        redisCachePresenter = new RedisCacheModulePresenter<>();
        redisCachePresenter.onAttachView(RedisCacheModule.this);
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.RedisCache.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        redisCachePresenter.onModuleRefresh();
    }

    @Override
    public RedisCacheNode createNode(Node parent, String sid, NodeContent content) {
        return new RedisCacheNode(this, sid, content);
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        redisCachePresenter.onNodeDelete(sid, id, node);
    }
}
