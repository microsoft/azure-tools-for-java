/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azuretools.core.mvp.model.rediscache;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.HashMap;
import java.util.List;

public class AzureRedisMvpModel {

    private AzureRedisMvpModel() {
    }

    private static final class AzureMvpModelHolder {
        private static final AzureRedisMvpModel INSTANCE = new AzureRedisMvpModel();
    }

    public static AzureRedisMvpModel getInstance() {
        return AzureMvpModelHolder.INSTANCE;
    }

    /**
     * Get all redis caches.
     * @return A map containing RedisCaches with subscription id as the key
     */
    public HashMap<String, RedisCaches> getRedisCaches() {
        HashMap<String, RedisCaches> redisCacheMaps = new HashMap<>();
        List<Subscription> subscriptions = AzureMvpModel.getInstance().getSelectedSubscriptions();
        for (Subscription subscription : subscriptions) {
            Azure azure = AuthMethodManager.getInstance().getAzureClient(subscription.subscriptionId());
            if (azure.redisCaches() == null) {
                continue;
            }
            redisCacheMaps.put(subscription.subscriptionId(), azure.redisCaches());
        }
        return redisCacheMaps;
    }

    /**
     * Get a Redis Cache by Id.
     * @param sid Subscription Id
     * @param id Redis cache's id
     * @return Redis Cache Object
     */
    public RedisCache getRedisCache(String sid, String id) {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        RedisCaches redisCaches = azure.redisCaches();
        if (redisCaches == null) {
            return null;
        }
        return redisCaches.getById(id);
    }

    /**
     * Delete a redis cache.
     * @param sid Subscription Id
     * @param id Redis cache's id
     */
    public void deleteRedisCache(String sid, String id) {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(sid);
        RedisCaches redisCaches = azure.redisCaches();
        if (redisCaches == null) {
            return;
        }
        redisCaches.deleteById(id);
    }
}
