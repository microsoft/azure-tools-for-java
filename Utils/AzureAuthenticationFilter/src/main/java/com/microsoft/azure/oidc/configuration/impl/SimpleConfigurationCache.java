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
