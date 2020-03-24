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
