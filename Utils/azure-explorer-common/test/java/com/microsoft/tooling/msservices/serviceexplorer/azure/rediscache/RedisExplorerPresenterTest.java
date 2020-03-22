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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azuretools.core.mvp.model.rediscache.RedisConnectionPools;
import com.microsoft.azuretools.core.mvp.model.rediscache.RedisExplorerMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.core.mvp.ui.base.TestSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisScanResult;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisValueData;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    RedisExplorerMvpModel.class,
    RedisExplorerPresenter.class,
    DefaultLoader.class,
    RedisConnectionPools.class,
})
public class RedisExplorerPresenterTest {

    @Mock
    private RedisExplorerMvpView redisExplorerMvpViewMock;

    @Mock
    private RedisExplorerMvpModel redisExplorerMvpModelMock;

    @Mock
    private ScanResult<String> stringScanResultMock;

    @Mock
    private ScanResult<Entry<String, String>> entryScanResultMock;

    @Mock
    private RedisScanResult redisScanResultMock;

    @Mock
    private RedisConnectionPools redisConnectionPoolsMock;

    private RedisExplorerPresenter<RedisExplorerMvpView> redisExplorerPresenter = new RedisExplorerPresenter<RedisExplorerMvpView>();
    private TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    private IDEHelper mockIDEHelper = new MockIDEHelper();

    private final static String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private final static String MOCK_ID = "test-id";
    private final static String MOCK_CURSOR = "0";
    private final static String MOCK_PATTERN = "*";
    private final static String MOCK_KEY = "key";

    private final static String TYPE_STRING = "string";
    private final static String TYPE_LIST = "list";
    private final static String TYPE_SET = "set";
    private final static String TYPE_ZSET = "zset";
    private final static String TYPE_HASH = "hash";

    private final static int MOCK_DB = 0;

    @Before
    public void setUp() throws Exception {
        redisExplorerPresenter.onAttachView(redisExplorerMvpViewMock);
        redisExplorerPresenter.initializeResourceData(MOCK_SUBSCRIPTION, MOCK_ID);
        SchedulerProviderFactory.getInstance().init(testSchedulerProvider);

        PowerMockito.mockStatic(RedisExplorerMvpModel.class);
        when(RedisExplorerMvpModel.getInstance()).thenReturn(redisExplorerMvpModelMock);
        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getIdeHelper()).thenReturn(mockIDEHelper);
    }

    @Test
    public void testOnReadDbNum() throws Exception {
        when(redisExplorerMvpModelMock.getDbNumber(MOCK_SUBSCRIPTION, MOCK_ID)).thenReturn(0);
        redisExplorerPresenter.onReadDbNum();

        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).renderDbCombo(MOCK_DB);
    }

    @Test
    public void testOnDbSelect() throws Exception {
        when(redisExplorerMvpModelMock.scanKeys(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_CURSOR, MOCK_PATTERN)).thenReturn(stringScanResultMock);
        redisExplorerPresenter.onDbSelect(MOCK_DB);
        PowerMockito.whenNew(RedisScanResult.class).withAnyArguments().thenReturn(redisScanResultMock);

        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showScanResult(Mockito.any(RedisScanResult.class));
    }

    @Test
    public void testOnKeyList() throws Exception {
        when(redisExplorerMvpModelMock.scanKeys(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_CURSOR, MOCK_PATTERN)).thenReturn(stringScanResultMock);
        redisExplorerPresenter.onKeyList(MOCK_DB, MOCK_CURSOR, MOCK_PATTERN);
        PowerMockito.whenNew(RedisScanResult.class).withAnyArguments().thenReturn(redisScanResultMock);

        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showScanResult(Mockito.any(RedisScanResult.class));
    }

    @Test
    public void testOnkeySelectWithStringKey() throws Exception {
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_STRING);
        when(redisExplorerMvpModelMock.getStringValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn("");

        redisExplorerPresenter.onkeySelect(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnkeySelectWithListKey() throws Exception {
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_LIST);
        when(redisExplorerMvpModelMock.getListValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(new ArrayList<String>());

        redisExplorerPresenter.onkeySelect(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnkeySelectWithSetKey() throws Exception {
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_SET);
        when(redisExplorerMvpModelMock.getSetValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY, MOCK_CURSOR)).thenReturn(stringScanResultMock);

        redisExplorerPresenter.onkeySelect(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnkeySelectWithZSetKey() throws Exception {
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_ZSET);
        when(redisExplorerMvpModelMock.getZSetValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(new HashSet<Tuple>());

        redisExplorerPresenter.onkeySelect(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnkeySelectWithHashKey() throws Exception {
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_HASH);
        when(redisExplorerMvpModelMock.getHashValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY, MOCK_CURSOR)).thenReturn(entryScanResultMock);

        redisExplorerPresenter.onkeySelect(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnGetKeyAndValue() throws Exception {
        when(redisExplorerMvpModelMock.checkKeyExistance(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(true);
        when(redisExplorerMvpModelMock.getKeyType(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(TYPE_STRING);
        when(redisExplorerMvpModelMock.getStringValue(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn("");

        redisExplorerPresenter.onGetKeyAndValue(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).updateKeyList();
        verify(redisExplorerMvpViewMock).showContent(Mockito.any(RedisValueData.class));
    }

    @Test
    public void testOnGetKeyAndValueWithNoneExistKey() throws Exception {
        when(redisExplorerMvpModelMock.checkKeyExistance(MOCK_SUBSCRIPTION, MOCK_ID, MOCK_DB, MOCK_KEY)).thenReturn(false);

        redisExplorerPresenter.onGetKeyAndValue(MOCK_DB, MOCK_KEY);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).getKeyFail();
    }

    @Test
    public void testOnRelease() {
        PowerMockito.mockStatic(RedisConnectionPools.class);
        when(RedisConnectionPools.getInstance()).thenReturn(redisConnectionPoolsMock);

        redisExplorerPresenter.onRelease(MOCK_ID);
        verify(redisConnectionPoolsMock).releasePool(Mockito.eq(MOCK_ID));
    }

    @After
    public void tearDown() {
        redisExplorerPresenter.onDetachView();
    }
}
