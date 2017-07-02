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
import com.microsoft.azuretools.core.mvp.model.AzureMvpModelHelper;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProvider;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class RedisCacheModulePresenter<V extends RedisCacheModule> extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBCROPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REDIS_ID = "Cannot get Redis Cache's ID.";
    private static final String CANNOT_DELETE_REDIS = "Cannot delete Redis Cache.";
    
    public RedisCacheModulePresenter(SchedulerProvider schedulerProvider) {
        super(schedulerProvider);
    }

    private final AzureMvpModelHelper azureMvpModelHelper = AzureMvpModelHelper.getInstance();

    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        HashMap<String, ArrayList<NodeContent>> nodeMap = new HashMap<String, ArrayList<NodeContent>>();
        try {
            HashMap<String, RedisCaches> redisCachesMap = azureMvpModelHelper.getRedisCaches();
            for (String sid : redisCachesMap.keySet()) {
                ArrayList<NodeContent> nodeContentList = new ArrayList<NodeContent>();
                for (RedisCache redisCache : redisCachesMap.get(sid).list()) {
                    nodeContentList
                            .add(new NodeContent(redisCache.id(), redisCache.name(), redisCache.provisioningState()));
                }
                nodeMap.put(sid, nodeContentList);
            }
        } catch (IOException e) {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_ID, e);
            return;
        }
        getMvpView().showNode(nodeMap);
    }

    /**
     * Called from view when the view need delete its child node.
     * 
     * @param sid
     *            subscription id
     * @param id
     *            resource id
     * @param node
     *            node reference
     */
    public void onNodeDelete(String sid, String id, Node node) {
        if (sid == null || sid.trim().isEmpty()) {
            getMvpView().onError(CANNOT_GET_SUBCROPTION_ID);
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            getMvpView().onError(CANNOT_GET_REDIS_ID);
            return;
        }
        try {
            azureMvpModelHelper.deleteRedisCache(sid, id);
        } catch (IOException e) {
            getMvpView().onErrorWithException(CANNOT_DELETE_REDIS, e);
            return;
        }
        getMvpView().removeDirectChildNode(node);
    }
}
