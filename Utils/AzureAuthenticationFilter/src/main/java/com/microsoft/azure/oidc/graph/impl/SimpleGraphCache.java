/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.graph.impl;

import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheService;
import com.microsoft.azure.oidc.concurrent.cache.impl.SimpleConcurrentCacheService;
import com.microsoft.azure.oidc.future.FutureHelper;
import com.microsoft.azure.oidc.future.impl.SimpleFutureHelper;
import com.microsoft.azure.oidc.graph.GraphCache;
import com.microsoft.azure.oidc.graph.GraphService;

public class SimpleGraphCache implements GraphCache {
    private static final GraphCache INSTANCE = new SimpleGraphCache();

    private final GraphService springGraphService = SimpleGraphService.getInstance();

    private final FutureHelper futureHelper = SimpleFutureHelper.getInstance();

    private final ConcurrentCacheService concurrentCacheService = SimpleConcurrentCacheService.getInstance();

    @Override
    public Boolean isUserInRole(String userID, String role) {
        final String key = String.format("%s:%s", userID, role);
        final Boolean entry = concurrentCacheService.getCache(Boolean.class, "roleCache").get(key);
        if (entry != null) {
            return entry;
        }
        final Boolean result = futureHelper.getResult(springGraphService.isUserInRoleAsync(userID, role));
        if (result == null) {
            return result;
        }
        concurrentCacheService.getCache(Boolean.class, "roleCache").putIfAbsent(key, result);
        return result;
    }

    public static GraphCache getInstance() {
        return INSTANCE;
    }
}
