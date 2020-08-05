/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class RedisCacheModule extends AzureRefreshableNode {
    private static final String REDIS_SERVICE_MODULE_ID = RedisCacheModule.class.getName();
    private static final String ICON_PATH = "RedisCache.svg";
    private static final String BASE_MODULE_NAME = "Redis Caches";
    private final RedisCacheModulePresenter<RedisCacheModule> redisCachePresenter;

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
