/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.concurrent.cache;

public interface ConcurrentCacheFactory<K, V> {

    ConcurrentCache<K, V> createConcurrentCache(Long ttl, Long maxSize);
}
