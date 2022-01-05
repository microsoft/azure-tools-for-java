/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.redis.AzureRedis;
import com.microsoft.azure.toolkit.redis.RedisCache;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class RedisResourceDefinition extends AzureServiceResource.Definition<RedisCache> implements SpringSupported<RedisCache> {
    public static final RedisResourceDefinition INSTANCE = new RedisResourceDefinition();

    private RedisResourceDefinition() {
        super("Azure.Redis", "Azure Redis Cache", "/icons/rediscache.svg");
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
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        properties.add(Pair.of("spring.redis.host", String.format("${%s_HOST}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.redis.port", String.format("${%s_PORT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.redis.password", String.format("${%s_KEY}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.redis.ssl", String.format("${%s_SSL}", Connection.ENV_PREFIX)));
        return properties;
    }

    @Override
    public RedisCache getResource(String dataId) {
        final ResourceId resourceId = ResourceId.fromString(dataId);
        final String rg = resourceId.resourceGroupName();
        return Objects.requireNonNull(Azure.az(AzureRedis.class).get(resourceId.subscriptionId(), rg)).caches().get(resourceId.name(), rg);
    }

    @Override
    public AzureFormJPanel<Resource<RedisCache>> getResourcePanel(Project project) {
        return new RedisResourcePanel();
    }

    public static class RegisterActivity extends PreloadingActivity {
        @Override
        public void preload(@NotNull ProgressIndicator progressIndicator) {
            ResourceManager.registerDefinition(INSTANCE);
        }
    }
}
