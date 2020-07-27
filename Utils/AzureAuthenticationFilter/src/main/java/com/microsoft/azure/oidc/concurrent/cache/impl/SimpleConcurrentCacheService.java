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
