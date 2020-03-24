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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.microsoft.azure.management.resources.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.Sku;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.core.mvp.model.rediscache.AzureRedisMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.rest.RestClient;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.tooling.IntegrationTestBase;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AuthMethodManager.class, AzureManager.class, SubscriptionManager.class, DefaultLoader.class })
@Ignore
public class RedisCacheCreateIntegrationTest extends IntegrationTestBase {

    @Mock
    private AuthMethodManager authMethodManagerMock;

    @Mock
    private AzureManager azureManagerMock;

    @Mock
    private SubscriptionManager subscriptionManagerMock;

    @Mock
    private Subscription subscriptionMock;

    @Mock
    private SubscriptionDetail subscriptionDetailMock;

    @Mock
    private UIHelper uiHelper;

    private Azure azure;

    private RedisCacheModule redisModule;

    private String defaultSubscription;

    private SubscriptionDetail currentsub;

    private final LinkedHashMap<String, String> skus = RedisCacheUtil.initSkus();
    private final LinkedHashMap<String, String> priceTier = initPriceTier();
    private String redisCacheQueryString;

    RedisCreateConfig BasicNewResGrpConfig = new RedisCreateConfig("MonaC1BasicNew", "East US", "MonaC1BasicNewRg",
            priceTier.get("BASIC1"), false, true);
    RedisCreateConfig BasicNewResGrpConfigNonSsl = new RedisCreateConfig("MonaC2BasicNewNonSsl", "East US",
            "MonaC2BasicNewNonSslRg", priceTier.get("BASIC2"), true, true);
    RedisCreateConfig BasicExistedResGrpConfig = new RedisCreateConfig("MonaC3BasicExist", "East US", "MonaExist",
            priceTier.get("BASIC3"), false, false);
    RedisCreateConfig BasicExistedResGrpConfigNonSsl = new RedisCreateConfig("MonaC4BasicExistNonSsl", "East US",
            "MonaExist", priceTier.get("BASIC4"), true, false);

    RedisCreateConfig StdNewResGrpConfig = new RedisCreateConfig("MonaC3StdNew", "East US", "MonaC3StdNewRg",
            priceTier.get("STD3"), false, true);
    RedisCreateConfig StdNewResGrpConfigNonSsl = new RedisCreateConfig("MonaC4StdNewNonSsl", "East US",
            "MonaC4StdNewNonSslRg", priceTier.get("STD4"), true, true);
    RedisCreateConfig StdExistedResGrpConfig = new RedisCreateConfig("MonaC5StdExist", "East US", "MonaExist",
            priceTier.get("STD5"), false, false);
    RedisCreateConfig StdExistedResGrpConfigNonSsl = new RedisCreateConfig("MonaC6StdExistNonSsl", "East US",
            "MonaExist", priceTier.get("STD6"), true, false);

    RedisCreateConfig PremNewResGrpConfig = new RedisCreateConfig("MonaP1New", "East US", "MonaP1NewRg",
            priceTier.get("PREMIUM1"), false, true);
    RedisCreateConfig PremNewResGrpConfigNonSsl = new RedisCreateConfig("MonaP2NewNonSsl", "East US",
            "MonaP2NewNonSslRg", priceTier.get("PREMIUM2"), true, true);
    RedisCreateConfig PremExistedResGrpConfig = new RedisCreateConfig("MonaP3Exist", "East US", "MonaExist",
            priceTier.get("PREMIUM3"), false, false);
    RedisCreateConfig PremExistedResGrpConfigNonSsl = new RedisCreateConfig("MonaP4ExistNonSsl", "East US", "MonaExist",
            priceTier.get("PREMIUM4"), true, false);

    RedisCreateConfig DupNameConfig = new RedisCreateConfig("MonaP1New", "East US", "MonaC3StdNewRg",
            priceTier.get("STD3"), false, true);
    RedisCreateConfig RgNotExistConfig = new RedisCreateConfig("MonaP3Exist", "East US", "NotExistRg",
            priceTier.get("PREMIUM3"), false, false);

    @Before
    public void setUp() throws Exception {
        setUpStep();
    }

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    // basic redisCache test case
    @Test
    public void testRedisCacheCreateBasicWithNewResGrp() throws Exception {
        createRedisTest(redisModule, BasicNewResGrpConfig);
    }

    @Test
    public void testRedisCacheCreateBasicWithNewResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, BasicNewResGrpConfigNonSsl);
    }

    @Test
    public void testRedisCacheCreateBasicWithExistedResGrp() throws Exception {
        createRedisTest(redisModule, BasicExistedResGrpConfig);
    }

    @Test
    public void testRedisCacheCreateBasicWithExistedResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, BasicExistedResGrpConfigNonSsl);
    }

    // Standard redisCache test case
    @Test
    public void testRedisCacheCreateStdWithNewResGrp() throws Exception {
        createRedisTest(redisModule, StdNewResGrpConfig);
    }

    @Test
    public void testRedisCacheCreateStdWithNewResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, StdNewResGrpConfigNonSsl);
    }

    @Test
    public void testRedisCacheCreateStdWithExistedResGrp() throws Exception {
        createRedisTest(redisModule, StdExistedResGrpConfig);
    }

    @Test
    public void testRedisCacheCreateStdWithExistedResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, StdExistedResGrpConfigNonSsl);
    }

    // Premium redisCache test case
    @Test
    public void testRedisCacheCreatePremWithNewResGrp() throws Exception {
        createRedisTest(redisModule, PremNewResGrpConfig);
    }

    @Test
    public void testRedisCacheCreatePremWithNewResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, PremNewResGrpConfigNonSsl);
    }

    @Test
    public void testRedisCacheCreatePremWithExistedResGrp() throws Exception {
        createRedisTest(redisModule, PremExistedResGrpConfig);
    }

    @Test
    public void testRedisCacheCreatePremWithExistedResGrpNonSsl() throws Exception {
        createRedisTest(redisModule, PremExistedResGrpConfigNonSsl);
    }

    // failed case
    @Test
    public void testRedisCacheCreateDupName() throws Exception {
        expectedEx.expect(com.microsoft.azure.CloudException.class);
        CreateRedisCache(azureManagerMock, currentsub, PremNewResGrpConfig);
        CreateRedisCache(azureManagerMock, currentsub, DupNameConfig);
    }

    @Test
    public void testRedisCacheCreateRgNotExist() throws Exception {
        expectedEx.expect(com.microsoft.azure.CloudException.class);
        CreateRedisCache(azureManagerMock, currentsub, RgNotExistConfig);
    }

    @After
    public void tearDown() throws Exception {
        resetTest(name.getMethodName());
    }

    @Override
    protected void initialize(RestClient restClient, String defaultSubscription, String domain) throws Exception {
        Azure.Authenticated azureAuthed = Azure.authenticate(restClient, defaultSubscription, domain);
        azure = azureAuthed.withSubscription(defaultSubscription);
        this.defaultSubscription = defaultSubscription;

        PowerMockito.mockStatic(AuthMethodManager.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureClient(MOCK_SUBSCRIPTION)).thenReturn(azure);
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        final Map<String, Subscription> mockSidToSubscriptionMap = new HashMap<>();
        mockSidToSubscriptionMap.put(MOCK_SUBSCRIPTION, subscriptionMock);
        final Map<String, SubscriptionDetail> mockSidToSubDetailMap = new HashMap<>();
        mockSidToSubDetailMap.put(MOCK_SUBSCRIPTION, subscriptionDetailMock);
        when(subscriptionDetailMock.isSelected()).thenReturn(true);
        when(subscriptionDetailMock.getSubscriptionId()).thenReturn(MOCK_SUBSCRIPTION);
        when(subscriptionManagerMock.getSubscriptionIdToSubscriptionDetailsMap()).thenReturn(mockSidToSubDetailMap);
        when(subscriptionManagerMock.getSubscriptionIdToSubscriptionMap()).thenReturn(mockSidToSubscriptionMap);
        when(subscriptionMock.subscriptionId()).thenReturn(MOCK_SUBSCRIPTION);

        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getUIHelper()).thenReturn(uiHelper);
        when(uiHelper.isDarkTheme()).thenReturn(false);

        currentsub = new SubscriptionDetail(defaultSubscription, defaultSubscription, defaultSubscription, true);
        redisCacheQueryString = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Cache/Redis/%s";

        // create a resource group for nonmock case
        if (!IS_MOCKED) {
            if (!azure.resourceGroups().checkExistence("MonaExist")) {
                azure.resourceGroups().define("MonaExist").withRegion("eastus");
            }
        }

        redisModule = new RedisCacheModule(null) {
            protected void loadActions() {
            }
        };

        Node.setNode2Actions(new HashMap<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>>());
    }

    public LinkedHashMap<String, String> initPriceTier() {
        final LinkedHashMap<String, String> Tier = new LinkedHashMap<String, String>();
        for (String key : skus.keySet()) {
            Tier.put(skus.get(key), key);
        }
        return Tier;
    }

    public void createRedisTest(RedisCacheModule redisModule, RedisCreateConfig config) throws Exception {
        redisModule.refreshItems();
        int redisCount = redisModule.getChildNodes().size();
        redisModule.removeAllChildNodes();
        redisModule.refreshItems();
        assertEquals(redisModule.getChildNodes().size(), redisCount);
        CreateRedisCache(azureManagerMock, currentsub, config);
        redisModule.removeAllChildNodes();
        redisModule.refreshItems();
        assertEquals(redisModule.getChildNodes().size(), redisCount + 1);

        // verify redisCache properties
        String redisCacheId = String.format(redisCacheQueryString, defaultSubscription, config.selectedResGrpValue,
                config.dnsNameValue);
        RedisCache redisCacheInstance = AzureRedisMvpModel.getInstance().getRedisCache(defaultSubscription,
                redisCacheId);
        assertEquals(redisCacheInstance.name(), config.dnsNameValue);
        assertEquals(redisCacheInstance.resourceGroupName(), config.selectedResGrpValue);
        assertEquals(redisCacheInstance.regionName(), config.selectedLocationValue);
        assertEquals(redisCacheInstance.nonSslPort(), config.noSSLPort);

        Sku skuVal = redisCacheInstance.sku();
        String tier = skus.get(config.selectedPriceTierValue).replace("STD", "STANDARD");
        assertEquals(skuVal.name().toString().toUpperCase() + Integer.toString(skuVal.capacity()), tier);

        RedisAccessKeys accessKeys = redisCacheInstance.getKeys();
        assertNotNull(accessKeys.primaryKey());
        assertNotNull(accessKeys.secondaryKey());

        // delete redis Cache
        redisModule.refreshItems();
        ObservableList<Node> nodes = redisModule.getChildNodes();
        RedisCacheNode redisCacheNode = null;
        for (Node node : nodes) {
            if (node.getName().equals(config.dnsNameValue)) {
                redisCacheNode = (RedisCacheNode) node;
            }
        }
        if (redisCacheNode == null) {
            throw new Exception("can't find Node" + config.dnsNameValue);
        }

        redisModule.removeNode(this.defaultSubscription, redisCacheNode.getResourceId(), redisCacheNode);
        redisModule.removeAllChildNodes();
        redisModule.refreshItems();
        assertEquals(redisModule.getChildNodes().size(), redisCount);
        Thread.sleep(1000);
    }

    public void CreateRedisCache(AzureManager azureManager, SubscriptionDetail currentSub, RedisCreateConfig config)
            throws Exception {
        Azure azure = AuthMethodManager.getInstance().getAzureClient(currentSub.getSubscriptionId());
        ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(azure, skus, config.dnsNameValue,
                config.selectedLocationValue, config.selectedResGrpValue, config.selectedPriceTierValue,
                config.noSSLPort, config.newResGrp);
        processor.process();
    }

    private class RedisCreateConfig {
        public RedisCreateConfig(String dnsNameValue, String selectedLocationValue, String selectedResGrpValue,
                String selectedPriceTierValue, boolean noSSLPort, boolean newResGrp) {
            this.dnsNameValue = dnsNameValue;
            this.selectedLocationValue = selectedLocationValue;
            this.selectedResGrpValue = selectedResGrpValue;
            this.selectedPriceTierValue = selectedPriceTierValue;
            this.noSSLPort = noSSLPort;
            this.newResGrp = newResGrp;
        }

        public String dnsNameValue;
        public String selectedResGrpValue;
        public String selectedLocationValue;
        public String selectedPriceTierValue;
        public boolean noSSLPort;
        public boolean newResGrp;
    }

}
