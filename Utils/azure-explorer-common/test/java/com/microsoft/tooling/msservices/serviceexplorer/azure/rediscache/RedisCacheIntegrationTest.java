package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
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

import junit.framework.Assert;

@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    AuthMethodManager.class, 
    AzureManager.class, 
    SubscriptionManager.class,
    DefaultLoader.class
})

public class RedisCacheIntegrationTest extends IntegrationTestBase {
    
    @Mock
    private AuthMethodManager authMethodManagerMock;
    
    @Mock
    private AzureManager azureManagerMock;
    
    @Mock
    private SubscriptionManager subscriptionManagerMock;
    
    @Mock
    private UIHelper uiHelper;
    
    private Azure azure;
    
    private RedisCacheModule redisModule;
    
    private String defaultSubscription;
    
    @Before
    public void setup() throws Exception {
        setUpStep();
    }
    
    @Test
    public void testRedisCache() {
        try {
            redisModule.refreshItems();
            ObservableList<Node> nodes = redisModule.getChildNodes();
            assertEquals(1, nodes.size());
            // TODO: add remove/create case
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            fail();
        }
    }
    
    @After
    public void cleanup() throws Exception {
        resetTest(name.getMethodName());
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
        when(uiHelper.isDarkTheme()).thenReturn(false);

        redisModule = new RedisCacheModule(null){
            protected void loadActions(){}
        };
        
        Node.setNode2Actions(new HashMap<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener> > >());
    }
}
