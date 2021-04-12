/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.concurrent.cache;

public interface ConcurrentCacheService {

    <V> ConcurrentCache<String, V> createCache(Class<V> clazzV, String name, Long ttl, Long maxSize);

    <V> ConcurrentCache<String, V> getCache(Class<V> clazzV, String name);

    void shutdownNow();

}
