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

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import static org.mockito.ArgumentMatchers.anyString;
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

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.core.mvp.model.rediscache.AzureRedisMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.core.mvp.ui.base.TestSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AzureRedisMvpModel.class,
    RedisPropertyViewPresenter.class,
    DefaultLoader.class,
})
public class RedisPropertyViewPresenterTest {

    @Mock
    private RedisPropertyMvpView redisPropertyMvpViewMock;

    @Mock
    private AzureRedisMvpModel azureRedisMvpModelMock;

    @Mock
    private RedisCacheProperty redisCachePropertyMock;

    private RedisPropertyViewPresenter<RedisPropertyMvpView> redisPropertyViewPresenter;

    private final static String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private final static String MOCK_ID = "test-id";

    private IDEHelper mockIDEHelper = new MockIDEHelper();
    private RedisCache redisCacheMock = new RedisCacheMock();

    @Before
    public void setUp() throws Exception {
        redisPropertyViewPresenter = new RedisPropertyViewPresenter<RedisPropertyMvpView>();
        redisPropertyViewPresenter.onAttachView(redisPropertyMvpViewMock);

        PowerMockito.mockStatic(AzureRedisMvpModel.class);
        when(AzureRedisMvpModel.getInstance()).thenReturn(azureRedisMvpModelMock);
        when(azureRedisMvpModelMock.getRedisCache(anyString(), anyString())).thenReturn(redisCacheMock);
        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getIdeHelper()).thenReturn(mockIDEHelper);
    }

    @Test
    public void testGetRedisProperty() throws Exception {
        TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
        SchedulerProviderFactory.getInstance().init(testSchedulerProvider);
        redisPropertyViewPresenter.onGetRedisProperty(MOCK_SUBSCRIPTION, MOCK_ID);
        testSchedulerProvider.triggerActions();

        verify(redisPropertyMvpViewMock).showProperty(Mockito.any(RedisCacheProperty.class));
    }

    @After
    public void tearDown() {
        redisPropertyViewPresenter.onDetachView();
    }
}
