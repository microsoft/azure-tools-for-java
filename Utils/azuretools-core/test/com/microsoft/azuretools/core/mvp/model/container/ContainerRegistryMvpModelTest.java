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

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.containerregistry.AccessKeyType;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerregistry.RegistryCredentials;
import com.microsoft.azure.management.containerregistry.implementation.RegistryInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.rest.RestException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private Registry registryMock1;

    @Mock
    private Registry registryMock2;

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
        when(registriesMock.getById(MOCK_REGISTRY_ID)).thenReturn(registryMock1);
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
    public void testListRegistryBySubscriptionId() {
        List<Registry> storedList = new PagedList<Registry>() {
            @Override
            public Page<Registry> nextPage(String nextPageLink) throws RestException, IOException {
                return null;
            }
        };
        storedList.add(registryMock1);
        storedList.add(registryMock2);
        when(registriesMock.list()).thenReturn((PagedList<Registry>)storedList);

        List<ResourceEx<Registry>> registryList = containerRegistryMvpModel.listRegistryBySubscriptionId(MOCK_SUBSCRIPTION_ID, false);
        assertEquals(2, registryList.size());
        verify(registriesMock, times(1)).list();
        reset(registriesMock);

        containerRegistryMvpModel.listRegistryBySubscriptionId(MOCK_SUBSCRIPTION_ID, false);
        verify(registriesMock, times(0)).list();

        when(registriesMock.list()).thenReturn((PagedList<Registry>)storedList);
        containerRegistryMvpModel.listRegistryBySubscriptionId(MOCK_SUBSCRIPTION_ID, true);
        verify(registriesMock, times(1)).list();
    }

    @Test
    public void testListContainerRegistries() throws IOException {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Subscription sub1 = mock(Subscription.class); when(sub1.subscriptionId()).thenReturn("1");
        Subscription sub2 = mock(Subscription.class); when(sub2.subscriptionId()).thenReturn("2");
        Subscription sub3 = mock(Subscription.class); when(sub3.subscriptionId()).thenReturn("3");
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        ContainerRegistryMvpModel mockModel = spy(containerRegistryMvpModel);
        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);
        when(registriesMock.list()).thenReturn(new PagedList<Registry>() {
            @Override
            public Page<Registry> nextPage(String nextPageLink) throws RestException, IOException {
                return null;
            }
        });

        mockModel.listContainerRegistries(false);
        verify(mockModel, times(0)).listRegistryBySubscriptionId(anyString(), eq(false));

        subscriptions.add(sub1);
        subscriptions.add(sub2);
        subscriptions.add(sub3);
        mockModel.listContainerRegistries(false);
        verify(mockModel, times(3)).listRegistryBySubscriptionId(anyString(), eq(false));
        reset(mockModel);

        mockModel.listContainerRegistries(true);
        verify(mockModel, times(3)).listRegistryBySubscriptionId(anyString(), eq(true));
    }

    @Test(expected = Exception.class)
    public void testCreateImageSettingWithRegistryWhenAdminOff() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(false);
        containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
    }

    @Test(expected = Exception.class)
    public void testCreateImageSettingWithRegistryWhenCredentialIsNull() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(true);
        when(registryMock1.getCredentials()).thenReturn(null);
        containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
    }

    @Test(expected = Exception.class)
    public void testCreateImageSettingWithRegistryWhenUserNameIsEmpty() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(true);
        RegistryCredentials credentials = mock(RegistryCredentials.class);
        when(registryMock1.getCredentials()).thenReturn(credentials);
        when(credentials.username()).thenReturn("");
        containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
    }

    @Test(expected = Exception.class)
    public void testCreateImageSettingWithRegistryWhenPasswordIsNull() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(true);
        RegistryCredentials credentials = mock(RegistryCredentials.class);
        when(registryMock1.getCredentials()).thenReturn(credentials);
        when(credentials.accessKeys()).thenReturn(null);
        containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
    }

    @Test(expected = Exception.class)
    public void testCreateImageSettingWithRegistryWhenPasswordIsEmpty() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(true);
        RegistryCredentials credentials = mock(RegistryCredentials.class);
        when(registryMock1.getCredentials()).thenReturn(credentials);
        Map<AccessKeyType, String> passwords = new HashMap<>();
        when(credentials.accessKeys()).thenReturn(passwords);
        containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
    }

    @Test
    public void testCreateImageSettingWithRegistry() throws Exception {
        when(registryMock1.adminUserEnabled()).thenReturn(true);
        RegistryCredentials credentials = mock(RegistryCredentials.class);
        when(registryMock1.getCredentials()).thenReturn(credentials);
        when(registryMock1.loginServerUrl()).thenReturn("url");
        when(credentials.username()).thenReturn("Alice");
        Map<AccessKeyType, String> passwords = new HashMap<>();
        passwords.put(AccessKeyType.PRIMARY, "primaryKey");
        when(credentials.accessKeys()).thenReturn(passwords);
        PrivateRegistryImageSetting setting = containerRegistryMvpModel.createImageSettingWithRegistry(registryMock1);
        assertEquals(setting.getUsername(), "Alice");
        assertEquals(setting.getPassword(), "primaryKey");
        assertEquals(setting.getServerUrl(), "url");
        assertEquals(setting.getImageNameWithTag(), "image:tag");
    }

    @Test
    public void testGetContainerRegistry() throws Exception {
        Registry registry = containerRegistryMvpModel.getContainerRegistry(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID);
        assertEquals(registryMock1, registry);
    }

    @Test(expected = Exception.class)
    public void testGetNonExistContainerRegistry() throws Exception {
        containerRegistryMvpModel.getContainerRegistry(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES, MOCK_REGISTRY_ID);
    }

    @Test
    public void testSetAdminUserEnabled() throws Exception {
        Registry.Update update = mock(Registry.Update.class);
        Registry.Update with = mock(Registry.Update.class);
        Registry.Update without = mock(Registry.Update.class);
        RegistryInner innerMock = mock(RegistryInner.class);
        when(registryMock1.update()).thenReturn(update);
        when(registryMock1.inner()).thenReturn(innerMock);
        when(update.withRegistryNameAsAdminUser()).thenReturn(with);
        when(update.withoutRegistryNameAsAdminUser()).thenReturn(without);

        Registry registry;
        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID, true);
        verify(with, times(1)).apply();
        assertEquals(registryMock1, registry);

        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID, MOCK_REGISTRY_ID, false);
        verify(without, times(1)).apply();
        assertEquals(registryMock1, registry);

        registry = containerRegistryMvpModel.setAdminUserEnabled(MOCK_SUBSCRIPTION_ID_WITHOUT_REGISTRIES, MOCK_REGISTRY_ID, true);
        assertEquals(null, registry);
    }
}