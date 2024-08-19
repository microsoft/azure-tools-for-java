package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.microsoft.azure.toolkit.intellij.dbtools.TelemetryConnectionInterceptor;
import static com.microsoft.azure.toolkit.intellij.redis.dbtools.RedisCacheParamEditor.KEY_REDIS_CACHE;

public class RedisConnectionInterceptor extends TelemetryConnectionInterceptor {
    protected RedisConnectionInterceptor() {
        super(KEY_REDIS_CACHE, "redis", "redis.connect_jdbc_from_dbtools");
    }
}
