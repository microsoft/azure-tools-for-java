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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import com.microsoft.azure.management.Azure;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.core.mvp.ui.base.TestSchedulerProvider;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisScanResult;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisValueData;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.rest.RestClient;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.tooling.IntegrationTestBase;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.UIHelper;

import junit.framework.Assert;

@PowerMockIgnore("javax.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AuthMethodManager.class, AzureManager.class, SubscriptionManager.class, DefaultLoader.class })
@Ignore
public class RedisExplorerIntegrationTest extends IntegrationTestBase {

    @Mock
    private AuthMethodManager authMethodManagerMock;

    @Mock
    private AzureManager azureManagerMock;

    @Mock
    private SubscriptionManager subscriptionManagerMock;

    @Mock
    private UIHelper uiHelper;

    @Mock
    private RedisExplorerMvpView redisExplorerMvpViewMock;

    private Azure azure;

    private String defaultSubscription;

    private RedisExplorerPresenter<RedisExplorerMvpView> redisExplorerPresenter = new RedisExplorerPresenter<RedisExplorerMvpView>();
    private TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();

    private IDEHelper mockIDEHelper = new MockIDEHelper();

    private String redisID;
    private final static String TEST_CURSOR = "0";
    private final static String TEST_PATTERN = "*";
    private final static int TEST_DB = 0;
    private final static int EMPTY_DB = 1;
    private final static String STRING_KEY = "testString";
    private final static String LIST_KEY = "testList";
    private final static String SET_KEY = "testSet";
    private final static String ZSET_KEY = "testZset";
    private final static String HASHSET_KEY = "testHashset";
    private final static String STRING_VALUE = "testStringValue";
    private final static String LIST_VALUE = "testListValue";
    private final static String SET_VALUE = "testSetValue";
    private final static String ZSET_VALUE = "testZsetValue";
    private final static String HASH_KEY = "testHashsetkey1";
    private final static String HASH_VALUE = "testHashsetValue";
    private final static String ZSET_SCORE_VALUE = "1.0";
    private static final String REDIS_NAME = "redisExploreTest";
    private static final String RESOURCE_GROUP = "redisExploreTestRg";

    @Before
    public void setUp() throws Exception {
        // this case only run in Nonmock mode, need to set up a redis database and and
        // add proper test data,
        // set VM argument: -DisMockedCase=false -DauthFilePath="c:\config.azureauth"

        assumeThat(IS_MOCKED, is(false));
        setUpStep();
        redisExplorerPresenter.onAttachView(redisExplorerMvpViewMock);
        redisExplorerPresenter.initializeResourceData(this.defaultSubscription, redisID);
        SchedulerProviderFactory.getInstance().init(testSchedulerProvider);
        when(DefaultLoader.getIdeHelper()).thenReturn(mockIDEHelper);
    }

    @Test
    public void testOnDbSelect() throws Exception {
        redisExplorerPresenter.onReadDbNum();
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).renderDbCombo(Mockito.anyInt());
    }

    @Test
    public void testScanPatternDefaultValue() throws Exception {
        List<String> expectedList = Arrays.asList(STRING_KEY, LIST_KEY, SET_KEY, ZSET_KEY, HASHSET_KEY);
        Collections.sort(expectedList);

        redisExplorerPresenter.onDbSelect(TEST_DB);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).showScanResult(Mockito.any(RedisScanResult.class));

        ArgumentCaptor<RedisScanResult> argument = ArgumentCaptor.forClass(RedisScanResult.class);
        verify(redisExplorerMvpViewMock).showScanResult(argument.capture());
        List<String> keys = argument.getValue().getKeys();
        Collections.sort(keys);
        assertEquals(keys, expectedList);

        redisExplorerPresenter.onDbSelect(EMPTY_DB);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock, times(2)).showScanResult(argument.capture());
        assertEquals(argument.getValue().getKeys().size(), 0);
    }

    @Test
    public void testScanMoreKey() throws Exception {
        final String testCursor = "3";
        List<String> expectedList = Arrays.asList(STRING_KEY, ZSET_KEY, HASHSET_KEY);
        Collections.sort(expectedList);

        redisExplorerPresenter.onKeyList(TEST_DB, testCursor, TEST_PATTERN);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisScanResult> argument = ArgumentCaptor.forClass(RedisScanResult.class);
        verify(redisExplorerMvpViewMock).showScanResult(argument.capture());

        List<String> keys = argument.getValue().getKeys();
        Collections.sort(keys);
        assertEquals(keys, expectedList);
    }

    @Test
    public void testScanPatternFilter() throws Exception {
        String patternFilter = "testS*";
        List<String> expectedList = Arrays.asList(STRING_KEY, SET_KEY);
        Collections.sort(expectedList);

        redisExplorerPresenter.onKeyList(TEST_DB, TEST_CURSOR, patternFilter);
        testSchedulerProvider.triggerActions();

        ArgumentCaptor<RedisScanResult> argument = ArgumentCaptor.forClass(RedisScanResult.class);
        verify(redisExplorerMvpViewMock).showScanResult(argument.capture());

        List<String> keys = argument.getValue().getKeys();
        Collections.sort(keys);
        assertEquals(keys, expectedList);

    }

    @Test
    public void testOnkeySelectWithStringKey() throws Exception {
        List<String> expectedList = Arrays.asList(STRING_VALUE);
        String expectedType = "STRING";

        redisExplorerPresenter.onkeySelect(TEST_DB, STRING_KEY);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisValueData> argument = ArgumentCaptor.forClass(RedisValueData.class);
        verify(redisExplorerMvpViewMock).showContent(argument.capture());

        ArrayList<String> values = flatList(argument.getValue().getRowData());
        Collections.sort(values);
        assertEquals(values, expectedList);
        assertEquals(argument.getValue().getKeyType().toString(), expectedType);

    }

    @Test
    public void testOnkeySelectWithListKey() throws Exception {
        String index = "1";
        List<String> expectedList = Arrays.asList(index, LIST_VALUE);
        String expectedType = "LIST";
        Collections.sort(expectedList);

        redisExplorerPresenter.onkeySelect(TEST_DB, LIST_KEY);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisValueData> argument = ArgumentCaptor.forClass(RedisValueData.class);
        verify(redisExplorerMvpViewMock).showContent(argument.capture());

        ArrayList<String> values = flatList(argument.getValue().getRowData());
        Collections.sort(values);
        assertEquals(values, expectedList);
        assertEquals(argument.getValue().getKeyType().toString(), expectedType);
    }

    @Test
    public void testOnkeySelectWithSetKey() {
        List<String> expectedList = Arrays.asList(SET_VALUE);
        String expectedType = "SET";
        Collections.sort(expectedList);

        redisExplorerPresenter.onkeySelect(TEST_DB, SET_KEY);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisValueData> argument = ArgumentCaptor.forClass(RedisValueData.class);
        verify(redisExplorerMvpViewMock).showContent(argument.capture());

        ArrayList<String> values = flatList(argument.getValue().getRowData());
        Collections.sort(values);
        assertEquals(values, expectedList);
        assertEquals(argument.getValue().getKeyType().toString(), expectedType);
    }

    @Test
    public void testOnkeySelectWithZsetKey() {
        List<String> expectedList = Arrays.asList(ZSET_SCORE_VALUE, ZSET_VALUE);
        String expectedType = "ZSET";
        Collections.sort(expectedList);

        redisExplorerPresenter.onkeySelect(TEST_DB, ZSET_KEY);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisValueData> argument = ArgumentCaptor.forClass(RedisValueData.class);
        verify(redisExplorerMvpViewMock).showContent(argument.capture());

        ArrayList<String> values = flatList(argument.getValue().getRowData());
        Collections.sort(values);
        assertEquals(values, expectedList);
        assertEquals(argument.getValue().getKeyType().toString(), expectedType);
    }

    @Test
    public void testOnkeySelectWithHashKey() {
        List<String> expectedList = Arrays.asList(HASH_KEY, HASH_VALUE);
        String expectedType = "HASH";
        Collections.sort(expectedList);

        redisExplorerPresenter.onkeySelect(TEST_DB, HASHSET_KEY);
        testSchedulerProvider.triggerActions();
        ArgumentCaptor<RedisValueData> argument = ArgumentCaptor.forClass(RedisValueData.class);
        verify(redisExplorerMvpViewMock).showContent(argument.capture());

        ArrayList<String> values = flatList(argument.getValue().getRowData());
        Collections.sort(values);
        assertEquals(values, expectedList);
        assertEquals(argument.getValue().getKeyType().toString(), expectedType);
    }

    @Test
    public void testOnGetKeyAndValueWithNoneExistKey() {
        String nonExistedKey = "nonExisted";
        redisExplorerPresenter.onGetKeyAndValue(TEST_DB, nonExistedKey);
        testSchedulerProvider.triggerActions();
        verify(redisExplorerMvpViewMock).getKeyFail();
        verify(redisExplorerMvpViewMock, times(0)).showContent(null);
    }

    @After
    public void tearDown() throws Exception {
        if (!IS_MOCKED) {
            resetTest(name.getMethodName());
        }
    }

    private ArrayList<String> flatList(ArrayList<String[]> valueData) {
        ArrayList<String> result = new ArrayList<String>();
        for (String[] valueList : valueData) {
            for (String value : valueList) {
                result.add(value);
            }
        }
        return result;
    }

    @Override
    protected void initialize(RestClient restClient, String defaultSubscription, String domain) throws Exception {
        Azure.Authenticated azureAuthed = Azure.authenticate(restClient, defaultSubscription, domain);
        azure = azureAuthed.withSubscription(defaultSubscription);
        this.defaultSubscription = defaultSubscription;
        Set<String> sidList = Stream.of(defaultSubscription).collect(Collectors.toSet());

        PowerMockito.mockStatic(AuthMethodManager.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getAzure(anyString())).thenReturn(azure);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        when(subscriptionManagerMock.getAccountSidList()).thenReturn(sidList);

        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getUIHelper()).thenReturn(uiHelper);

        String redisCacheQueryString = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Cache/Redis/%s";
        redisID = String.format(redisCacheQueryString, defaultSubscription, RESOURCE_GROUP, REDIS_NAME);

    };
}
