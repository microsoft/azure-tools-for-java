/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class BaseRedisResourceDefinition extends AzureServiceResource.Definition<RedisCache> {
    public BaseRedisResourceDefinition() {
        super("Azure.Redis", "Azure Redis Cache", AzureIcons.RedisCache.MODULE.getIconPath());
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<RedisCache> redisDef, Project project) {
        final RedisCache redis = redisDef.getData();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_HOST", Connection.ENV_PREFIX), redis.getHostName());
        env.put(String.format("%s_PORT", Connection.ENV_PREFIX), String.valueOf(redis.getSSLPort()));
        env.put(String.format("%s_SSL", Connection.ENV_PREFIX), String.valueOf(redis.isNonSslPortEnabled()));
        env.put(String.format("%s_KEY", Connection.ENV_PREFIX), redis.getPrimaryKey());
        return env;
    }

    @Override
    public RedisCache getResource(String dataId, final String id) {
        return Azure.az(AzureRedis.class).getById(dataId);
    }

    @Override
    public List<Resource<RedisCache>> getResources(Project project) {
        return Azure.az(AzureRedis.class).list().stream()
            .flatMap(m -> m.caches().list().stream())
            .map(this::define).toList();
    }
}
