package com.microsoft.azuretools.core.mvp.model.rediscache;

import com.microsoft.azure.management.redis.RedisAccessKeys;

public class RedisAccessKeysMock implements RedisAccessKeys {

    private static final String MOCK_STRING = "test";

    public String primaryKey() {
        return MOCK_STRING;
    }

    public String secondaryKey() {
        return MOCK_STRING;
    }
}
