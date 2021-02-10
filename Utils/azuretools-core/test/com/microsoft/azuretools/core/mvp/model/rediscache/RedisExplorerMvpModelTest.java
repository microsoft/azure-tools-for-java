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
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    RedisConnectionPools.class,
})
public class RedisExplorerMvpModelTest {

    @Mock
    private RedisConnectionPools redisConnectionPoolsMock;

    @Mock
    private Jedis jedisMock;

    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_REDIS_ID = "test-id";
    private static final int MOCK_DB = 0;
    private static final String MOCK_CURSOR = "0";
    private static final String MOCK_PATTERN = "*";
    private static final String MOCK_KEY = "key";
    private static final long MOCK_LEN = 10L;
    private static final String DATABASE_COMMAND = "databases";


    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(RedisConnectionPools.class);
        when(RedisConnectionPools.getInstance()).thenReturn(redisConnectionPoolsMock);
        when(redisConnectionPoolsMock.getJedis(anyString(), anyString())).thenReturn(jedisMock);
    }

    @After
    public void tearDown() {
        redisConnectionPoolsMock = null;
        jedisMock = null;
    }

    @Test
    public void testGetDbNumber() throws Exception {
        RedisExplorerMvpModel.getInstance().getDbNumber(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(jedisMock, times(1)).configGet(Mockito.eq(DATABASE_COMMAND));
    }

    @Test
    public void testScanKeys() throws Exception {
        RedisExplorerMvpModel.getInstance().scanKeys(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_CURSOR, MOCK_PATTERN);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).scan(Mockito.eq(MOCK_CURSOR), Mockito.any(ScanParams.class));
    }

    @Test
    public void testGetKeyType() throws Exception {
        RedisExplorerMvpModel.getInstance().getKeyType(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).type(Mockito.eq(MOCK_KEY));
    }

    @Test
    public void testGetStringValue() throws Exception {
        RedisExplorerMvpModel.getInstance().getStringValue(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).get(Mockito.eq(MOCK_KEY));
    }

    @Test
    public void testGetListValue() throws Exception {
        when(jedisMock.llen(anyString())).thenReturn(MOCK_LEN);

        RedisExplorerMvpModel.getInstance().getListValue(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).lrange(Mockito.eq(MOCK_KEY), Mockito.eq(0L), Mockito.eq(MOCK_LEN));
    }

    @Test
    public void testGetSetValue() throws Exception {
        RedisExplorerMvpModel.getInstance().getSetValue(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY, MOCK_CURSOR);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).sscan(Mockito.eq(MOCK_KEY), Mockito.eq(MOCK_CURSOR), Mockito.any(ScanParams.class));
    }

    @Test
    public void testGetZSetValue() throws Exception {
        when(jedisMock.zcard(anyString())).thenReturn(MOCK_LEN);

        RedisExplorerMvpModel.getInstance().getZSetValue(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).zrangeWithScores(Mockito.eq(MOCK_KEY), Mockito.eq(0L), Mockito.eq(MOCK_LEN));
    }

    @Test
    public void testGetHashValue() throws Exception {
        RedisExplorerMvpModel.getInstance().getHashValue(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY, MOCK_CURSOR);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).hscan(Mockito.eq(MOCK_KEY), Mockito.eq(MOCK_CURSOR), Mockito.any(ScanParams.class));
    }

    @Test
    public void testCheckKeyExistance() throws Exception {
        RedisExplorerMvpModel.getInstance().checkKeyExistance(MOCK_SUBSCRIPTION, MOCK_REDIS_ID, MOCK_DB, MOCK_KEY);
        verify(jedisMock, times(1)).select(Mockito.eq(MOCK_DB));
        verify(jedisMock, times(1)).exists(Mockito.eq(MOCK_KEY));
    }
}
