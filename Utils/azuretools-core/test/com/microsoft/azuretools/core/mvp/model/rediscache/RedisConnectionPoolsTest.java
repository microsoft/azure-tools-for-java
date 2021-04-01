/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.rediscache;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AzureRedisMvpModel.class,
    RedisConnectionPools.class,
})
public class RedisConnectionPoolsTest {

    @Mock
    private Jedis jedisMock;

    @Mock
    private JedisPool jedisPoolMock;

    @Mock
    private AzureRedisMvpModel azureRedisMvpModelMock;

    @Mock
    private RedisCache redisCacheMock;

    @Mock
    private RedisAccessKeys redisAccessKeysMock;

    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_REDIS_ID = "test-id";
    private static final String MOCK_RETURN_STRING = "RedisTest";
    private static final int MOCK_PORT = 6380;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AzureRedisMvpModel.class);
        when(AzureRedisMvpModel.getInstance()).thenReturn(azureRedisMvpModelMock);
        when(azureRedisMvpModelMock.getRedisCache(anyString(), anyString())).thenReturn(redisCacheMock);

        when(redisCacheMock.hostName()).thenReturn(MOCK_RETURN_STRING);
        when(redisCacheMock.keys()).thenReturn(redisAccessKeysMock);
        when(redisAccessKeysMock.primaryKey()).thenReturn(MOCK_RETURN_STRING);
        when(redisCacheMock.sslPort()).thenReturn(MOCK_PORT);
        when(jedisPoolMock.getResource()).thenReturn(jedisMock);
        PowerMockito.whenNew(JedisPool.class).withAnyArguments().thenReturn(jedisPoolMock);
    }

    @After
    public void tearDown() {
        jedisMock = null;
        jedisPoolMock = null;
        azureRedisMvpModelMock = null;
        redisCacheMock = null;
        redisAccessKeysMock = null;
    }


    @Test
    public void testGetAndReleaseJedis() throws Exception {
        RedisConnectionPools.getInstance().getJedis(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(jedisPoolMock, times(1)).getResource();
        RedisConnectionPools.getInstance().releasePool(MOCK_REDIS_ID);
        verify(jedisPoolMock, times(1)).destroy();
    }

    @Test
    public void testReleaseNonExistedJedis() {
        // Just release without getJedis
        RedisConnectionPools.getInstance().releasePool(MOCK_REDIS_ID);
        verify(jedisPoolMock, times(0)).destroy();
    }
}
