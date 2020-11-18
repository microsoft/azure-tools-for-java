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

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.rest.RestException;
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AuthMethodManager.class,
        AzureManager.class,
        Azure.class,
        WebApps.class,
        SubscriptionManager.class,
        AppServiceManager.class,
        AzureMvpModel.class
})

public class AzureWebAppMvpModelTest {
    private AzureWebAppMvpModel azureWebAppMvpModel;

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
    private WebApps webAppsMock;

    @Mock
    private AppServiceManager appSrvMgr;

    @Mock
    private AppServicePlans appSrvPlans;

    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";

    @Before
    public void setUp() throws IOException {
        PowerMockito.mockStatic(AuthMethodManager.class);
        PowerMockito.mockStatic(AzureMvpModel.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureClient(MOCK_SUBSCRIPTION)).thenReturn(azureMock);
        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        when(azureMock.webApps()).thenReturn(webAppsMock);
        when(azureMock.appServices()).thenReturn(appSrvMgr);
        when(appSrvMgr.appServicePlans()).thenReturn(appSrvPlans);
        when(AzureMvpModel.getInstance()).thenReturn(mvpModel);
        azureWebAppMvpModel = AzureWebAppMvpModel.getInstance();
    }

    @After
    public void tearDown() {
        azureWebAppMvpModel.clearWebAppsCache();
        reset(webAppsMock);
        reset(azureMock);
        reset(authMethodManagerMock);
    }

    @Test
    public void testGetWebAppById() {
        WebApp app = mock(WebApp.class);
        when(app.toString()).thenReturn("testApp");
        when(webAppsMock.getById("test")).thenReturn(app);

        try {
            WebApp result = azureWebAppMvpModel.getWebAppById(MOCK_SUBSCRIPTION, "test");
            assert (result.toString()).equals("testApp");
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    @Ignore
    public void testCreateWebAppNewSrvPlan() {
        WebAppSettingModel settingModel = new WebAppSettingModel();
        settingModel.setCreatingAppServicePlan(true);
        settingModel.setJdkVersion(JavaVersion.JAVA_1_8_0_111);
        settingModel.setWebContainer("Tomcat 8.5");
        settingModel.setCreatingResGrp(true);
        settingModel.setResourceGroup("testRgp");
        settingModel.setPricing("1_2");
        settingModel.setSubscriptionId(MOCK_SUBSCRIPTION);

        AppServicePlan.DefinitionStages.WithCreate withCreate = mock(AppServicePlan.DefinitionStages.WithCreate.class);
        AppServicePlan.DefinitionStages.Blank srvPlanDef = mock(AppServicePlan.DefinitionStages.Blank.class);
        AppServicePlan.DefinitionStages.WithGroup grp = mock(AppServicePlan.DefinitionStages.WithGroup.class);
        AppServicePlan.DefinitionStages.WithPricingTier withPricing = mock(AppServicePlan.DefinitionStages.WithPricingTier.class);
        AppServicePlan.DefinitionStages.WithOperatingSystem withOS = mock(AppServicePlan.DefinitionStages.WithOperatingSystem.class);
        when(azureMock.appServices()).thenReturn(appSrvMgr);
        when(appSrvPlans.define(settingModel.getAppServicePlanName())).thenReturn(srvPlanDef);
        when(srvPlanDef.withRegion(settingModel.getRegion())).thenReturn(grp);
        when(grp.withNewResourceGroup(settingModel.getResourceGroup())).thenReturn(withPricing);
        when(withPricing.withPricingTier(any(PricingTier.class))).thenReturn(withOS);
        when(withOS.withOperatingSystem(OperatingSystem.WINDOWS)).thenReturn(withCreate);

        WebApp.DefinitionStages.Blank def = mock(WebApp.DefinitionStages.Blank.class);
        WebApp.DefinitionStages.WithNewAppServicePlan with = mock(WebApp.DefinitionStages.WithNewAppServicePlan.class);
        WebApp.DefinitionStages.NewAppServicePlanWithGroup withGrp = mock(WebApp.DefinitionStages.NewAppServicePlanWithGroup.class);
        when(webAppsMock.define(settingModel.getWebAppName())).thenReturn(def);
        when(def.withRegion(settingModel.getRegion())).thenReturn(withGrp);
        when(withGrp.withNewResourceGroup(settingModel.getResourceGroup())).thenReturn(with);

        try {
            azureWebAppMvpModel.createWebAppOnWindows(settingModel);
        } catch (Exception e) {
        }

        verify(with, times(1)).withNewWindowsPlan(withCreate);
    }

    @Test
    public void testCreateWebAppExistingSrvPlan() {
        WebAppSettingModel settingModel = new WebAppSettingModel();
        settingModel.setCreatingAppServicePlan(false);
        settingModel.setJdkVersion(JavaVersion.JAVA_1_8_0_111);
        settingModel.setWebContainer("Tomcat 8.5");
        settingModel.setCreatingResGrp(false);
        settingModel.setResourceGroup("testRgp");
        settingModel.setPricing("1_2");
        settingModel.setSubscriptionId(MOCK_SUBSCRIPTION);

        AppServicePlan srvPlan = mock(AppServicePlan.class);
        when(appSrvPlans.getById(settingModel.getAppServicePlanId())).thenReturn(srvPlan);
        WebApp.DefinitionStages.Blank def = mock(WebApp.DefinitionStages.Blank.class);
        when(webAppsMock.define(settingModel.getWebAppName())).thenReturn(def);
        WebApp.DefinitionStages.ExistingWindowsPlanWithGroup withGrp = mock(WebApp.DefinitionStages.ExistingWindowsPlanWithGroup.class);
        when(def.withExistingWindowsPlan(srvPlan)).thenReturn(withGrp);

        try {
            azureWebAppMvpModel.createWebAppOnWindows(settingModel);
        } catch (Exception e) {
        }

        verify(withGrp, times(1)).withExistingResourceGroup(anyString());
        verify(def, times(1)).withExistingWindowsPlan(any(AppServicePlan.class));
    }

    @Test
    public void testCreateWebAppOnLinuxExistingSrvPlan() {
        WebAppOnLinuxDeployModel model = new WebAppOnLinuxDeployModel();
        model.setCreatingNewAppServicePlan(false);
        model.setCreatingNewResourceGroup(false);
        model.setAppServicePlanId("test");
        model.setResourceGroupName("testRgp");
        model.setWebAppName("testApp");
        model.setPricingSkuSize("500");
        model.setPricingSkuTier("tier1");
        model.setSubscriptionId(MOCK_SUBSCRIPTION);

        PrivateRegistryImageSetting imageSetting = new PrivateRegistryImageSetting("url",
                "name", "password", "imageNameWithTag",
                "startUpFile");
        model.setPrivateRegistryImageSetting(imageSetting);

        WebApp.DefinitionStages.Blank def = mock(WebApp.DefinitionStages.Blank.class);
        when(webAppsMock.define(model.getWebAppName())).thenReturn(def);
        AppServicePlan srvPlan = mock(AppServicePlan.class);
        when(appSrvPlans.getById(model.getAppServicePlanId())).thenReturn(srvPlan);
        WebApp.DefinitionStages.ExistingLinuxPlanWithGroup withGrp = mock(WebApp.DefinitionStages.ExistingLinuxPlanWithGroup.class);
        when(def.withExistingLinuxPlan(srvPlan)).thenReturn(withGrp);

        try {
            azureWebAppMvpModel.createWebAppWithPrivateRegistryImage(model);
        } catch (Exception e) {
        }

        verify(withGrp, times(1)).withExistingResourceGroup(anyString());
        verify(def, times(1)).withExistingLinuxPlan(any(AppServicePlan.class));
    }

    @Test
    public void testRestartWebApp() {
        WebApp app = mock(WebApp.class);
        when(webAppsMock.getById("appId")).thenReturn(app);

        try {
            azureWebAppMvpModel.restartWebApp(MOCK_SUBSCRIPTION, "appId");
        } catch (Exception e) {
            printException(e);
        }

        verify(app, times(1)).restart();
    }

    @Test
    public void testStartWebApp() {
        WebApp app = mock(WebApp.class);
        when(webAppsMock.getById("appId")).thenReturn(app);

        try {
            azureWebAppMvpModel.startWebApp(MOCK_SUBSCRIPTION, "appId");
        } catch (Exception e) {
            printException(e);
        }

        verify(app, times(1)).start();
    }

    @Test
    public void testStopWebApp() {
        WebApp app = mock(WebApp.class);
        when(webAppsMock.getById("appId")).thenReturn(app);

        try {
            azureWebAppMvpModel.stopWebApp(MOCK_SUBSCRIPTION, "appId");
        } catch (Exception e) {
            printException(e);
        }

        verify(app, times(1)).stop();
    }

    @Test
    public void testListAppServicePlanBySubscriptionIdAndResourceGroupName() {
        List<AppServicePlan> storedList = new PagedList<AppServicePlan>() {
            @Override
            public Page<AppServicePlan> nextPage(String nextPageLink) throws RestException, IOException {
                return null;
            }
        };
        AppServicePlan plan1 = mock(AppServicePlan.class);
        AppServicePlan plan2 = mock(AppServicePlan.class);
        storedList.add(plan1);
        storedList.add(plan2);
        when(appSrvPlans.listByResourceGroup("testGrp")).thenReturn((PagedList<AppServicePlan>) storedList);
        List<AppServicePlan> srvPlanList = azureWebAppMvpModel.listAppServicePlanBySubscriptionIdAndResourceGroupName(MOCK_SUBSCRIPTION, "testGrp");
        assertEquals(2, srvPlanList.size());
    }

    @Test
    public void testListAppServicePlanBySubscriptionId() {
        try {
            azureWebAppMvpModel.listAppServicePlanBySubscriptionId(MOCK_SUBSCRIPTION);
        } catch (Exception e) {
            printException(e);
        }

        verify(appSrvPlans, times(1)).list(true);
    }

    @Test
    @Ignore
    public void testListAllWebApps() throws IOException {
        List<WebApp> webAppList = prepareMockWebAppList();
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) webAppList);

        final List<Subscription> subscriptions = new ArrayList<Subscription>();
        final Subscription sub = mock(Subscription.class);
        when(sub.subscriptionId()).thenReturn("1");
        subscriptions.add(sub);
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);
        final AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);

        mockWebAppModel.listAllWebApps(false);
        List<ResourceEx<WebApp>> resultList = azureWebAppMvpModel.listAllWebApps(false);

        verify(mockWebAppModel, times(1)).listWebApps("1", false);
        assertEquals(2, resultList.size());

        final WebApp app3 = mock(WebApp.class);
        when(app3.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);
        webAppList.add(app3);
        resultList = azureWebAppMvpModel.listAllWebApps(true);
        assertEquals(3, resultList.size());

        reset(webAppsMock);
    }

    @Test
    @Ignore
    public void testListWebAppsOnLinux() throws IOException {
        final List<WebApp> storedList = prepareMockWebAppList();
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) storedList);

        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);

        final List<ResourceEx<WebApp>> webAppList = azureWebAppMvpModel.listWebAppsOnLinux(MOCK_SUBSCRIPTION, true);
        final AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        mockWebAppModel.listWebAppsOnLinux(MOCK_SUBSCRIPTION, true);
        verify(mockWebAppModel, times(1)).listWebAppsOnLinux(MOCK_SUBSCRIPTION, true);
        assertEquals(1, webAppList.size());
    }

    @Test
    @Ignore
    public void testListAllWebAppsOnLinux() throws IOException {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);

        mockWebAppModel.listAllWebAppsOnLinux(true);
        verify(mockWebAppModel, times(1)).listAllWebAppsOnLinux(true);
        List<ResourceEx<WebApp>> resultList = azureWebAppMvpModel.listAllWebAppsOnLinux(true);
        assertEquals(resultList.size(), 0);

        final Subscription sub = mock(Subscription.class);
        when(sub.subscriptionId()).thenReturn("1");
        subscriptions.add(sub);

        final List<WebApp> webAppList = prepareMockWebAppList();
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) webAppList);

        resultList = azureWebAppMvpModel.listAllWebAppsOnLinux(true);
        assertEquals(resultList.size(), 1);
    }

    @Test
    @Ignore
    public void testListWebAppsOnWindows() throws IOException {
        List<WebApp> storedList = new PagedList<WebApp>() {
            @Override
            public Page<WebApp> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        WebApp app1 = mock(WebApp.class);
        when(app1.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);
        WebApp app2 = mock(WebApp.class);
        when(app2.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);
        storedList.add(app1);
        storedList.add(app2);
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) storedList);

        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);

        final List<ResourceEx<WebApp>> rstList = azureWebAppMvpModel.listWebAppsOnWindows(MOCK_SUBSCRIPTION, true);
        final AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        mockWebAppModel.listWebAppsOnWindows(MOCK_SUBSCRIPTION, true);
        verify(mockWebAppModel, times(1)).listWebAppsOnWindows(MOCK_SUBSCRIPTION, true);
        assertEquals(2, rstList.size());
    }

    @Test
    @Ignore
    public void testListAllWebAppsOnWindows() throws IOException {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);

        mockWebAppModel.listAllWebAppsOnWindows(true);
        verify(mockWebAppModel, times(1)).listAllWebAppsOnWindows(true);
        List<ResourceEx<WebApp>> resultList = azureWebAppMvpModel.listAllWebAppsOnWindows(true);
        assertEquals(resultList.size(), 0);

        final Subscription sub = mock(Subscription.class);
        when(sub.subscriptionId()).thenReturn("1");
        subscriptions.add(sub);

        final List<WebApp> webAppList = prepareMockWebAppList();
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) webAppList);

        resultList = azureWebAppMvpModel.listAllWebAppsOnWindows(true);
        assertEquals(resultList.size(), 1);
    }

    @Test
    public void testListWebContainers() {
        List<WebAppUtils.WebContainerMod> warContainers = AzureWebAppMvpModel.listWebContainersForWarFile();
        assertEquals(6, warContainers.size());

        List<WebAppUtils.WebContainerMod> jarContainers = AzureWebAppMvpModel.listWebContainersForJarFile(JdkModel.JAVA_8_NEWEST);
        assertEquals(1, jarContainers.size());
        assertEquals(jarContainers.get(0), WebAppUtils.WebContainerMod.Java_SE_8);

        jarContainers = AzureWebAppMvpModel.listWebContainersForJarFile(JdkModel.JAVA_ZULU_11_0_2);
        assertEquals(1, jarContainers.size());
        assertEquals(jarContainers.get(0), WebAppUtils.WebContainerMod.Java_SE_11);

        jarContainers = AzureWebAppMvpModel.listWebContainersForJarFile(JdkModel.JAVA_7_NEWEST);
        assertEquals(0, jarContainers.size());
    }

    @Test
    public void testListJdks() {
        assertEquals(JdkModel.values().length, azureWebAppMvpModel.listJdks().size());
    }

    private List<WebApp> prepareMockWebAppList() {
        final List<WebApp> webAppList = new PagedList<WebApp>() {
            @Override
            public Page<WebApp> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };
        final WebApp app1 = mock(WebApp.class);
        final WebApp app2 = mock(WebApp.class);
        when(app1.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);
        when(app2.operatingSystem()).thenReturn(OperatingSystem.LINUX);
        webAppList.add(app1);
        webAppList.add(app2);
        return webAppList;
    }

    private void printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stacktrace = sw.toString(); // stack trace as a string
        System.out.println(stacktrace);
    }
}
