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

package com.microsoft.azuretools.core.mvp.model.rediscache;

import com.microsoft.azure.management.redis.RedisCache;

import java.io.IOException;
import java.util.LinkedHashMap;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConnectionPools {

    private static final int TIMEOUT = 500;
    private static final int MAX_CONNECTIONS = 1;
    private static final String GANNOT_GET_RESID = "Cannot get Redis Cache from Azure.";

    private LinkedHashMap<String, JedisPool> pools;

    private RedisConnectionPools() {
        this.pools = new LinkedHashMap<String, JedisPool>(MAX_CONNECTIONS);
    }

    private static final class RedisConnectionFactoryHolder {
        private static final RedisConnectionPools INSTANCE = new RedisConnectionPools();
    }

    public static RedisConnectionPools getInstance() {
        return RedisConnectionFactoryHolder.INSTANCE;
    }

    /**
     * Get Jedis connection.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @return jedis connection
     * @throws IOException Error getting the Redis Cache
     */
    public synchronized Jedis getJedis(String sid, String id) throws Exception  {
        if (pools.get(id) == null) {
            if (pools.size() == MAX_CONNECTIONS) {
                releasePool(pools.keySet().iterator().next());
            }
            connect(sid, id);
        }
        return pools.get(id).getResource();
    }

    /**
     * Destroy the jedisPool.
     *
     * @param id
     *            id of the jedisPool which needs to be destroyed
     */
    public synchronized void releasePool(String id) {
        if (pools.containsKey(id)) {
            JedisPool jedisPool = pools.get(id);
            if (jedisPool != null) {
                jedisPool.destroy();
            }
            pools.remove(id);
        }
    }

    private void connect(String sid, String id) throws Exception {
        RedisCache redisCache = AzureRedisMvpModel.getInstance().getRedisCache(sid, id);

        if (redisCache == null) {
            throw new Exception(GANNOT_GET_RESID);
        }

        // get redis setting
        String hostName = redisCache.hostName();
        String password = redisCache.keys().primaryKey();
        int port = redisCache.sslPort();

        // create connection pool according to redis setting
        JedisPool pool = new JedisPool(new JedisPoolConfig(), hostName, port, TIMEOUT, password, true);
        pools.put(id, pool);
    }
}
