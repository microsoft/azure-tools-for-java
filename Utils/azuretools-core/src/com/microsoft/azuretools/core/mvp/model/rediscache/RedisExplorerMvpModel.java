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

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisException;

public class RedisExplorerMvpModel {

    private static final int DEFAULT_REDIS_DB_NUMBER = 16;
    private static final int MAX_DATABASE_NUMBER = 64;
    private static final int DEFAULT_KEY_COUNT = 50;
    private static final long DEFAULT_RANGE_START = 0;
    private static final int DEFAULT_VAL_COUNT = 500;

    private RedisExplorerMvpModel() {
    }

    private static final class RedisExplorerMvpModelHolder {
        private static final RedisExplorerMvpModel INSTANCE = new RedisExplorerMvpModel();
    }

    public static RedisExplorerMvpModel getInstance() {
        return RedisExplorerMvpModelHolder.INSTANCE;
    }

    /**
     * Get the number of databases the Redis Cache has.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @return the number of databases the Redis Cache has
     * @throws Exception
     *             Error getting the Redis Cache
     */
    public int getDbNumber(String sid, String id) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            try {
                List<String> dbs = jedis.configGet("databases");
                if (dbs.size() > 0) {
                    return Integer.parseInt(dbs.get(1));
                }
                return DEFAULT_REDIS_DB_NUMBER;
            } catch (JedisException e) {
                // Use binary search to determine the number of database the Redis has.
                int start = 0, end = MAX_DATABASE_NUMBER, mid;
                while (start < end) {
                    mid = start + (end - start) / 2;
                    if (canConnect(jedis, mid)) {
                        start = mid + 1;
                    } else {
                        end = mid;
                    }
                }
                return start;
            }
        }
    }

    public boolean checkKeyExistance(String sid, String id, int db, String key) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.exists(key);
        }
    }

    /**
     * Scan the keys with count defined in DEFAULT_KEY_COUNT
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param cursor
     *            cursor for Redis Scan command
     * @param pattern
     *            pattern for Redis Scan Param
     * @return Scan Result returned from Jedis
     * @throws Exception
     *
     */
    public ScanResult<String> scanKeys(String sid, String id, int db, String cursor, String pattern) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.scan(cursor, new ScanParams().match(pattern).count(DEFAULT_KEY_COUNT));
        }
    }

    /**
     * Get the type of the given key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @return type of the given key
     * @throws Exception
     */
    public String getKeyType(String sid, String id, int db, String key) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.type(key);
        }
    }

    /**
     * Get the value of a string type key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @return the value of a string type key
     * @throws Exception
     */
    public String getStringValue(String sid, String id, int db, String key) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.get(key);
        }
    }

    /**
     * Get the value of a list type key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @return the value of a list type key
     * @throws Exception
     */
    public List<String> getListValue(String sid, String id, int db, String key) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            long listLength = jedis.llen(key);
            return jedis.lrange(key, DEFAULT_RANGE_START,
                    listLength < DEFAULT_VAL_COUNT ? listLength : DEFAULT_VAL_COUNT);
        }
    }

    /**
     * Get the value of a set type key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @param cursor
     *            cursor for Redis Scan command
     * @return the value of a set type key
     * @throws Exception
     */
    public ScanResult<String> getSetValue(String sid, String id, int db, String key, String cursor) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.sscan(key, cursor, new ScanParams().count(DEFAULT_VAL_COUNT));
        }
    }

    /**
     * Get the value of a zset type key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @return the value of a zset type key
     * @throws Exception
     */
    public Set<Tuple> getZSetValue(String sid, String id, int db, String key) throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            long zsetLength = jedis.zcard(key);
            return jedis.zrangeWithScores(key, DEFAULT_RANGE_START,
                    zsetLength < DEFAULT_VAL_COUNT ? zsetLength : DEFAULT_VAL_COUNT);
        }
    }

    /**
     * Get the value of a hash type key.
     *
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            name of the key
     * @param cursor
     *            cursor for Redis Scan command
     * @return the value of a hash type key
     * @throws Exception
     */
    public ScanResult<Entry<String, String>> getHashValue(String sid, String id, int db, String key, String cursor)
            throws Exception {
        try (Jedis jedis = RedisConnectionPools.getInstance().getJedis(sid, id)) {
            jedis.select(db);
            return jedis.hscan(key, cursor, new ScanParams().count(DEFAULT_VAL_COUNT));
        }
    }

    private boolean canConnect(Jedis jedis, int index) {
        try {
            jedis.select(index);
            return true;
        } catch (JedisException ex) {
            return false;
        }
    }
}
