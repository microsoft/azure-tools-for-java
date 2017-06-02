/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.HashMap;

public final class RedisCacheModule extends AzureRefreshableNode implements RedisCacheMvpView {
    private static final String REDIS_SERVICE_MODULE_ID = com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule.class.getName();
    private static final String ICON_PATH = "RedisCache.png";
    private static final String BASE_MODULE_NAME = "Redis Caches";
    private final RedisCachePresenter<RedisCacheModule> redisCachePresenter;
    
    public RedisCacheModule(Node parent) {
        super(REDIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        redisCachePresenter = new RedisCachePresenter<RedisCacheModule>();
        redisCachePresenter.onAttachView(RedisCacheModule.this);
    }
    @Override
    protected void refreshItems() throws AzureCmdException {
    	redisCachePresenter.onRedisCacheRefresh();
    }
    
    @Override
    public void onRemoveNode(RedisCacheNode redisCacheNode) {
        removeDirectChildNode(redisCacheNode);
    }
    
	@Override
	public void onRefreshNode(HashMap<String, RedisCaches> redisCachesMap) {
		for (String sid: redisCachesMap.keySet()) {
			for (RedisCache redis: redisCachesMap.get(sid).list()) {
				addChildNode(new RedisCacheNode(this, sid, redisCachePresenter, redis));
			}
		}
	}
}
