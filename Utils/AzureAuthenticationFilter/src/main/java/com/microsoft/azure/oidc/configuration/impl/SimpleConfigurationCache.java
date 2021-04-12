/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.configuration.impl;

import com.microsoft.azure.oidc.concurrent.cache.ConcurrentCacheService;
import com.microsoft.azure.oidc.concurrent.cache.impl.SimpleConcurrentCacheService;
import com.microsoft.azure.oidc.configuration.Configuration;
import com.microsoft.azure.oidc.configuration.ConfigurationCache;
import com.microsoft.azure.oidc.configuration.ConfigurationLoader;
import com.microsoft.azure.oidc.future.FutureHelper;
import com.microsoft.azure.oidc.future.impl.SimpleFutureHelper;

public class SimpleConfigurationCache implements ConfigurationCache {
    private static final ConfigurationCache INSTANCE = new SimpleConfigurationCache();

    private final ConfigurationLoader configurationLoader = SimpleConfigurationLoader.getInstance();

    private final FutureHelper futureHelper = SimpleFutureHelper.getInstance();

    private final ConcurrentCacheService concurrentCacheService = SimpleConcurrentCacheService.getInstance();

    @Override
    public Configuration load() {
        final String key = "SINGLE";
        final Configuration entry = concurrentCacheService.getCache(Configuration.class, "configurationCache").get(key);
        if (entry != null) {
            return entry;
        }
        final Configuration result = futureHelper.getResult(configurationLoader.loadAsync());
        if (result == null) {
            return result;
        }
        concurrentCacheService.getCache(Configuration.class, "configurationCache").putIfAbsent(key, result);
        return result;
    }

    public static ConfigurationCache getInstance() {
        return INSTANCE;
    }
}
