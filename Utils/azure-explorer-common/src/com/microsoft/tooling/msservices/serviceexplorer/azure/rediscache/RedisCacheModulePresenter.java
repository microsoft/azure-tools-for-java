/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.core.mvp.model.rediscache.AzureRedisMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.ArrayList;
import java.util.HashMap;

public class RedisCacheModulePresenter<V extends RedisCacheModule> extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBCROPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REDIS_ID = "Cannot get Redis Cache's ID.";
    private static final String CANNOT_DELETE_REDIS = "Cannot delete Redis Cache.";

    private final AzureRedisMvpModel azureRedisMvpModel = AzureRedisMvpModel.getInstance();

    /**
     * Called from view when the view needs refresh.
     */
    public void onModuleRefresh() {
        final HashMap<String, ArrayList<NodeContent>> nodeMap = new HashMap<>();
        final HashMap<String, RedisCaches> redisCachesMap = azureRedisMvpModel.getRedisCaches();
        for (final String sid : redisCachesMap.keySet()) {
            final ArrayList<NodeContent> nodeContentList = new ArrayList<>();
            for (final RedisCache redisCache : redisCachesMap.get(sid).list()) {
                nodeContentList
                    .add(new NodeContent(redisCache.id(), redisCache.name(), redisCache.provisioningState()));
            }
            nodeMap.put(sid, nodeContentList);
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
        azureRedisMvpModel.deleteRedisCache(sid, id);
        getMvpView().removeDirectChildNode(node);
    }
}
