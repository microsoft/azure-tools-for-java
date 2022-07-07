/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.rediscache;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.redis.implementation.RedisManager;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;

import java.util.HashMap;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.Azure.az;

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
        List<Subscription> subscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        for (Subscription subscription : subscriptions) {
            final RedisManager.Configurable configurable = RedisManager.configure();
            final String sid = subscription.getId();
            final RedisManager azure = IdeAzureAccount.getInstance().authenticateForTrack1(sid, configurable, (t, c) -> c.authenticate(t, sid));
            if (azure.redisCaches() == null) {
                continue;
            }
            redisCacheMaps.put(sid, azure.redisCaches());
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
        final RedisManager.Configurable configurable = RedisManager.configure();
        final RedisManager azure = IdeAzureAccount.getInstance().authenticateForTrack1(sid, configurable, (t, c) -> c.authenticate(t, sid));
        final RedisCaches redisCaches = azure.redisCaches();
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
        final RedisManager.Configurable configurable = RedisManager.configure();
        final RedisManager azure = IdeAzureAccount.getInstance().authenticateForTrack1(sid, configurable, (t, c) -> c.authenticate(t, sid));
        RedisCaches redisCaches = azure.redisCaches();
        if (redisCaches == null) {
            return;
        }
        redisCaches.deleteById(id);
    }
}
