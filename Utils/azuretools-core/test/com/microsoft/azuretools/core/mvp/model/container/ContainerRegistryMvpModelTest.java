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
 *
 */

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AuthMethodManager.class,
        AzureManager.class,
        Azure.class,
        SubscriptionManager.class,
        AppServiceManager.class,
        AzureMvpModel.class
})
public class ContainerRegistryMvpModelTest {
    private static final String MOCK_SUBSCRIPTION_ID = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES = "00000000-0000-0000-0000-000000000001";
    private static final String MOCK_REGISTRY_ID = "MOCK_REGISTRY_ID";

    private ContainerRegistryMvpModel containerRegistryMvpModel;

    @Mock
    private AzureMvpModel mvpModel;

    @Mock
    private AuthMethodManager authMethodManagerMock;

    @Mock
    private Azure azureMock;

    @Mock
    private AzureManager azureManagerMock;

    @Mock
    private SubscriptionManager subscriptionManagerMock;

    @Mock
    private Registries registriesMock;

    @Mock
    private Registry registryMock;

    @Mock
    private Azure azureMockWithoutRegistries;

    @Mock
    private Subscription subscriptionWithoutRegistries;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(AuthMethodManager.class);
        PowerMockito.mockStatic(AzureMvpModel.class);
        when(azureMockWithoutRegistries.containerRegistries()).thenReturn(null);
        when(subscriptionWithoutRegistries.subscriptionId()).thenReturn(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureClient(MOCK_SUBSCRIPTION_ID)).thenReturn(azureMock);
        when(authMethodManagerMock.getAzureClient(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES)).thenReturn(azureMockWithoutRegistries);
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        when(azureMock.containerRegistries()).thenReturn(registriesMock);
        when(registriesMock.getById(MOCK_REGISTRY_ID)).thenReturn(registryMock);
        when(AzureMvpModel.getInstance()).thenReturn(mvpModel);
        containerRegistryMvpModel = ContainerRegistryMvpModel.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        reset(azureMock);
        reset(authMethodManagerMock);
    }

    @Test
    public void getInstance() throws Exception {
        assertEquals(ContainerRegistryMvpModel.getInstance(), containerRegistryMvpModel);
    }

    @Test
    public void getContainerRegistries() throws Exception {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Registries registries1 = mock(Registries.class);
        Registries registries2 = mock(Registries.class);
        Azure azure1 = mock(Azure.class); when(azure1.containerRegistries()).thenReturn(registries1);
        Azure azure2 = mock(Azure.class); when(azure2.containerRegistries()).thenReturn(registries2);
        Subscription sub1 = mock(Subscription.class); when(sub1.subscriptionId()).thenReturn("1");
        Subscription sub2 = mock(Subscription.class); when(sub2.subscriptionId()).thenReturn("2");
        when(authMethodManagerMock.getAzureClient("1")).thenReturn(azure1);
        when(authMethodManagerMock.getAzureClient("2")).thenReturn(azure2);
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        Map<String, Registries> emptyRegistriesMap = containerRegistryMvpModel.getContainerRegistries();
        assertEquals(0, emptyRegistriesMap.size());

        subscriptions.add(sub1);
        subscriptions.add(sub2);
        subscriptions.add(subscriptionWithoutRegistries);
        Map<String, Registries> registriesMap = containerRegistryMvpModel.getContainerRegistries();
        assertEquals(2, registriesMap.size());
        assertEquals(registries1, registriesMap.get(sub1.subscriptionId()));
        assertEquals(registries2, registriesMap.get(sub2.subscriptionId()));
    }

    @Test
    public void getContainerRegistry() throws Exception {
        Registry registry = containerRegistryMvpModel.getContainerRegistry(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID);
        assertEquals(registryMock, registry);


        Registry registryNull = containerRegistryMvpModel.getContainerRegistry(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES, MOCK_REGISTRY_ID);
        assertEquals(null, registryNull);
    }

    @Test
    public void setAdminUserEnabled() throws Exception {
        Registry.Update update = mock(Registry.Update.class);
        Registry.Update with = mock(Registry.Update.class);
        Registry.Update without = mock(Registry.Update.class);
        when(registryMock.update()).thenReturn(update);
        when(update.withRegistryNameAsAdminUser()).thenReturn(with);
        when(update.withoutRegistryNameAsAdminUser()).thenReturn(without);

        Registry registry;
        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID, true);
        verify(with, times(1)).apply();
        assertEquals(registryMock, registry);

        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID, false);
        verify(without, times(1)).apply();
        assertEquals(registryMock, registry);

        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES, MOCK_REGISTRY_ID, true);
        assertEquals(null, registry);
    }

}