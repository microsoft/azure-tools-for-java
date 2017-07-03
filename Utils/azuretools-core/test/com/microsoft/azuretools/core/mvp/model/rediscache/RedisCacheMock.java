package com.microsoft.azuretools.core.mvp.model.rediscache;

import java.util.Map;

import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCachePremium;
import com.microsoft.azure.management.redis.RedisKeyType;
import com.microsoft.azure.management.redis.Sku;
import com.microsoft.azure.management.redis.implementation.RedisManager;
import com.microsoft.azure.management.redis.implementation.RedisResourceInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import rx.Observable;

public class RedisCacheMock implements RedisCache{

    private static final String MOCK_STRING = "test";

    public String hostName() {
        return MOCK_STRING;
    }

    public RedisAccessKeys keys() {
        return new RedisAccessKeysMock();
    }

    public int sslPort() {
        return 0;
    }

    @Override
    public Region region() {
        return null;
    }

    @Override
    public String regionName() {
        return MOCK_STRING;
    }

    @Override
    public Map<String, String> tags() {
        return null;
    }

    @Override
    public String type() {
        return MOCK_STRING;
    }

    @Override
    public String key() {
        return MOCK_STRING;
    }

    @Override
    public String id() {
        return MOCK_STRING;
    }

    @Override
    public String name() {
        return MOCK_STRING;
    }

    @Override
    public String resourceGroupName() {
        return MOCK_STRING;
    }

    @Override
    public RedisManager manager() {
        return null;
    }

    @Override
    public RedisResourceInner inner() {
        return null;
    }

    @Override
    public RedisCache refresh() {
        return null;
    }

    @Override
    public Observable<RedisCache> refreshAsync() {
        return null;
    }

    @Override
    public Update update() {
        return null;
    }

    @Override
    public RedisCachePremium asPremium() {
        return null;
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public String provisioningState() {
        return MOCK_STRING;
    }

    @Override
    public int port() {
        return 0;
    }

    @Override
    public String redisVersion() {
        return MOCK_STRING;
    }

    @Override
    public Sku sku() {
        return null;
    }

    @Override
    public Map<String, String> redisConfiguration() {
        return null;
    }

    @Override
    public boolean nonSslPort() {
        return false;
    }

    @Override
    public int shardCount() {
        return 0;
    }

    @Override
    public String subnetId() {
        return MOCK_STRING;
    }

    @Override
    public String staticIP() {
        return MOCK_STRING;
    }

    @Override
    public RedisAccessKeys getKeys() {
        return new RedisAccessKeysMock();
    }


    @Override
    public RedisAccessKeys refreshKeys() {
        return new RedisAccessKeysMock();
    }

    @Override
    public RedisAccessKeys regenerateKey(RedisKeyType keyType) {
        return new RedisAccessKeysMock();
    }
}
