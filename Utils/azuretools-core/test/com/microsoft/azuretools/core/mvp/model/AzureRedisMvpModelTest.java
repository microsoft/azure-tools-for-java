/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.core.mvp.model.rediscache.AzureRedisMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AuthMethodManager.class,
    AzureManager.class,
    Azure.class,
    AzureMvpModel.class,
    RedisCaches.class,
    SubscriptionManager.class
})
@Ignore
public class AzureRedisMvpModelTest {

    private AzureRedisMvpModel azureRedisMvpModel;

    @Mock
    private AuthMethodManager authMethodManagerMock;

    @Mock
    private Azure azureMock;

    @Mock
    private AzureManager azureManagerMock;

    @Mock
    private Subscription subscriptionMock;

    @Mock
    private Subscription subscriptionDetailMock;

    @Mock
    private SubscriptionManager subscriptionManagerMock;

    @Mock
    private RedisCaches redisCachesMock;

    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_REDIS_ID = "test-id";

    @Before
    public void setUp() throws IOException {
        PowerMockito.mockStatic(AuthMethodManager.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureClient(MOCK_SUBSCRIPTION)).thenReturn(azureMock);
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);

        azureRedisMvpModel = AzureRedisMvpModel.getInstance();
    }

    @After
    public void tearDown() {
        azureRedisMvpModel = null;
        authMethodManagerMock = null;
        azureMock = null;
        redisCachesMock = null;
        subscriptionMock = null;
        subscriptionManagerMock = null;
    }

    @Test
    public void testGetRedisCaches() throws IOException {
        final int expectKeySetSize = 1;
        final Map<String, Subscription> mockSidToSubscriptionMap = new HashMap<>();
        mockSidToSubscriptionMap.put(MOCK_SUBSCRIPTION, subscriptionMock);
        final Map<String, Subscription> mockSidToSubDetailMap = new HashMap<>();
        mockSidToSubDetailMap.put(MOCK_SUBSCRIPTION, subscriptionDetailMock);

        when(subscriptionDetailMock.isSelected()).thenReturn(true);
        when(subscriptionDetailMock.getId()).thenReturn(MOCK_SUBSCRIPTION);
        when(subscriptionMock.getId()).thenReturn(MOCK_SUBSCRIPTION);
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);

        HashMap<String, RedisCaches> redisCachesMap = azureRedisMvpModel.getRedisCaches();

        assertNotNull(redisCachesMap);
        assertEquals(expectKeySetSize, redisCachesMap.keySet().size());
        assertNotNull(redisCachesMap.get(MOCK_SUBSCRIPTION));
    }

    @Test
    public void testDeleteRedisCache() throws IOException {
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);

        azureRedisMvpModel.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(1)).deleteById(MOCK_REDIS_ID);
    }

    @Test
    public void testDeleteRedisCacheWhenRedisCacheIsNull() throws IOException {
        when(azureMock.redisCaches()).thenReturn(null);

        azureRedisMvpModel.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).deleteById(MOCK_REDIS_ID);
    }

    @Test
    public void testGetRedisCache() throws IOException {
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);

        azureRedisMvpModel.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(1)).getById(MOCK_REDIS_ID);
    }

    @Test
    public void testGetRedisCacheWhenRedisCacheIsNull() throws IOException {
        when(azureMock.redisCaches()).thenReturn(null);

        azureRedisMvpModel.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).getById(MOCK_REDIS_ID);
    }
}
