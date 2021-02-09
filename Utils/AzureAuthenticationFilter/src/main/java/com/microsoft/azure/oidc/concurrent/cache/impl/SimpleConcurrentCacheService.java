/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.concurrent.cache.impl;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCache;
import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheFactory;
import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheService;

public class SimpleConcurrentCacheService implements ConcurrentCacheService {
    private final static ConcurrentCacheService INSTANCE = new SimpleConcurrentCacheService();

    private final ConcurrentCacheFactory<String, Object> concurrentCacheFactory = SimpleConcurrentCacheFactory
            .getInstance(String.class, Object.class);

    private final Map<String, ConcurrentCache<String, Object>> cacheMap = new HashMap<String, ConcurrentCache<String, Object>>();

    @SuppressWarnings("unchecked")
    @Override
    public <V> ConcurrentCache<String, V> createCache(Class<V> clazzV, String name, Long ttl, Long maxSize) {
        final ConcurrentCache<String, Object> concurrentCache = concurrentCacheFactory.createConcurrentCache(ttl, maxSize);
        cacheMap.put(name, concurrentCache);
        return (ConcurrentCache<String, V>) concurrentCache;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> ConcurrentCache<String, V> getCache(Class<V> classV, String name) {
        return (ConcurrentCache<String, V>) cacheMap.get(name);
    }

    @Override
    public void shutdownNow() {
        for(@SuppressWarnings("rawtypes") final ConcurrentCache cache: cacheMap.values()) {
            cache.shutdownNow();
        }
        cacheMap.clear();
    }

    public static ConcurrentCacheService getInstance() {
        return INSTANCE;
    }
}
