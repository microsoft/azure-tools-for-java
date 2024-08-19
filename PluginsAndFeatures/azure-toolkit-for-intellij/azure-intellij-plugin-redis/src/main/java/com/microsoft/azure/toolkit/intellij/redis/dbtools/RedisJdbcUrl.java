/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis.dbtools;

import com.microsoft.azure.toolkit.redis.RedisCache;

public class RedisJdbcUrl {
    public static String from(RedisCache redisCache) {
        return String.format("jdbc:redis://%s:%d?ssl=%b", redisCache.getHostName(), redisCache.getSSLPort(), true);
    }
}
