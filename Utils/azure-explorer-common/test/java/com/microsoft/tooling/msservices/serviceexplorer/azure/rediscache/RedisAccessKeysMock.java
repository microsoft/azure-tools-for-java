package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

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
