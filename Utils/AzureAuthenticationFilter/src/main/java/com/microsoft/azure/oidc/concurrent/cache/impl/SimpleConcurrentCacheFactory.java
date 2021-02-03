/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.concurrent.cache.impl;

import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCache;
import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheFactory;

public class SimpleConcurrentCacheFactory<K, V> implements ConcurrentCacheFactory<K, V> {
    @SuppressWarnings("rawtypes")
    private static final ConcurrentCacheFactory INSTANCE = new SimpleConcurrentCacheFactory();

    @Override
    public ConcurrentCache<K, V> createConcurrentCache(Long ttl, Long maxSize) {
        return new TTLConcurrentCache<K, V>(ttl, maxSize);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ConcurrentCacheFactory<K, V> getInstance(Class<K> clazzK, Class<V> clazzV) {
        return INSTANCE;
    }
}
