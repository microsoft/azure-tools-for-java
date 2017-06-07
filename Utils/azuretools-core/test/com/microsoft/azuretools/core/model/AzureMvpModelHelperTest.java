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

package com.microsoft.azuretools.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;


@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AuthMethodManager.class, 
    AzureManager.class, 
    Azure.class, 
    RedisCaches.class,
    SubscriptionManager.class
})

public class AzureMvpModelHelperTest {
    
    private AzureMvpModelHelper azureMvpModelHelper = null;
    
    @Mock
    private AuthMethodManager authMethodManagerMock;
    
    @Mock
    private AzureManager azureManagerMock;
    
    @Mock
    private Azure azureMock;
    
    @Mock 
    private RedisCaches redisCachesMock;
    
    @Mock
    private SubscriptionManager subscriptionManagerMock;
    
    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_REDIS_ID = "test-id";
    
    @Before
    public void setUp() throws IOException {
        PowerMockito.mockStatic(AuthMethodManager.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);

        azureMvpModelHelper = AzureMvpModelHelper.getInstance();
    }
    
    @After
    public void tearDown() {
        azureMvpModelHelper = null;
        authMethodManagerMock = null;
        azureManagerMock = null;
        azureMock = null;
        redisCachesMock = null;
        subscriptionManagerMock = null;
    }
    
    @Test
    public void testGetRedisCaches() throws IOException {
        final int expectKeySetSize = 1;
        final Set<String> mockSidList = Stream.of(MOCK_SUBSCRIPTION).collect(Collectors.toSet());
        
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azureMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        when(subscriptionManagerMock.getAccountSidList()).thenReturn(mockSidList);
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);
        
        HashMap<String, RedisCaches> redisCachesMap = azureMvpModelHelper.getRedisCaches();
        
        assertNotNull(redisCachesMap);
        assertEquals(expectKeySetSize, redisCachesMap.keySet().size());
        assertNotNull(redisCachesMap.get(MOCK_SUBSCRIPTION));
    }
    
    @Test
    public void testGetRedisCachesWhenAzureManagerIsNull() throws IOException {
        final int expectKeySetSize = 0;
        
        when(authMethodManagerMock.getAzureManager()).thenReturn(null);
        
        HashMap<String, RedisCaches> redisCachesMap = azureMvpModelHelper.getRedisCaches();
        assertNotNull(redisCachesMap);
        assertEquals(expectKeySetSize, redisCachesMap.keySet().size());
    }
    
    @Test
    public void testGetRedisCachesWhenSubscriptionManagerIsNull() throws IOException {
        final int expectKeySetSize = 0;
        
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(null);
        
        HashMap<String, RedisCaches> redisCachesMap = azureMvpModelHelper.getRedisCaches();
        assertNotNull(redisCachesMap);
        assertEquals(expectKeySetSize, redisCachesMap.keySet().size());
    }
    
    @Test
    public void testGetRedisCachesWhenAzureIsNull() throws IOException {
        final int expectKeySetSize = 0;
        
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(null);
        
        HashMap<String, RedisCaches> redisCachesMap = azureMvpModelHelper.getRedisCaches();
        assertNotNull(redisCachesMap);
        assertEquals(expectKeySetSize, redisCachesMap.keySet().size());
    }
    
    @Test
    public void testDeleteRedisCache() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azureMock);
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);
        
        azureMvpModelHelper.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(1)).deleteById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testDeleteRedisCacheWhenAzureManagerIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(null);
        
        azureMvpModelHelper.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).deleteById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testDeleteRedisCacheWhenAzureIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(null);
        
        azureMvpModelHelper.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).deleteById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testDeleteRedisCacheWhenRedisCacheIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azureMock);
        when(azureMock.redisCaches()).thenReturn(null);
        
        azureMvpModelHelper.deleteRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).deleteById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testGetRedisCache() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azureMock);
        when(azureMock.redisCaches()).thenReturn(redisCachesMock);
        
        azureMvpModelHelper.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(1)).getById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testGetRedisCacheWhenAzureManagerIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(null);
        
        azureMvpModelHelper.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).getById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testGetRedisCacheWhenAzureIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(null);
        
        azureMvpModelHelper.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).getById(MOCK_REDIS_ID);
    }
    
    @Test
    public void testGetRedisCacheWhenRedisCacheIsNull() throws IOException {
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azureMock);
        when(azureMock.redisCaches()).thenReturn(null);
        
        azureMvpModelHelper.getRedisCache(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
        verify(redisCachesMock, times(0)).getById(MOCK_REDIS_ID);
    }
}