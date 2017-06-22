/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.core.model.rediscache;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.core.model.AzureMvpModelHelper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConnectionPools {
    
    private static final int NON_SSL_PORT = 6379;
    private static final int TIMEOUT = 500;
    
    private Map<String, JedisPool> pools;

    private RedisConnectionPools() {
        this.pools = new HashMap<String, JedisPool>();
    }
    
    private static final class  RedisConnectionFactoryHolder {
        private static final RedisConnectionPools INSTANCE = new RedisConnectionPools();
    }
    
    public static RedisConnectionPools getInstance() {
        return RedisConnectionFactoryHolder.INSTANCE;
    }
    
    public synchronized Jedis getJedis(String sid, String id) throws IOException {
        if (pools.get(id) == null) {
            if (pools.size() > 0) {
                releaseAllPools();
            }
            connect(sid, id);
        }
        return pools.get(id).getResource();
    }
    
    public synchronized void releasePool(String id) {
        JedisPool jedisPool = pools.get(id);
        if (jedisPool != null) {
            jedisPool.destroy();
        }
        pools.remove(id);
    }
    
    private void connect(String sid, String id) throws IOException {
        RedisCache redisCache = AzureMvpModelHelper.getInstance().getRedisCache(sid, id);
        
        // get redis setting
        String hostName = redisCache.hostName();
        String password = redisCache.keys().primaryKey();
        boolean enableSsl = !redisCache.nonSslPort();
        int port = enableSsl ? redisCache.port() : NON_SSL_PORT;
        
        // create connection pool according to redis setting
        JedisPool pool = new JedisPool(new JedisPoolConfig(), hostName, port, TIMEOUT, password, enableSsl);
        pools.put(id, pool);
    }
    
    private void releaseAllPools() {
        for (String key: pools.keySet()) {
            releasePool(key);
        }
    }
}
