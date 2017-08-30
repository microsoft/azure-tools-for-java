package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.*;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
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
    private static final String MOCK_REDIS_ID = "test-id";

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
        azureWebAppMvpModel.cleanWebApps();
        azureWebAppMvpModel.cleanWebAppsOnLinux();
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
            assert(result.toString()).equals("testApp");
        } catch (IOException e) {
            fail();
        }
    }

    @Test
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
        WebApp.DefinitionStages.WithNewAppServicePlan with =  mock(WebApp.DefinitionStages.WithNewAppServicePlan.class);
        WebApp.DefinitionStages.NewAppServicePlanWithGroup withGrp = mock(WebApp.DefinitionStages.NewAppServicePlanWithGroup.class);
        when(webAppsMock.define(settingModel.getWebAppName())).thenReturn(def);
        when(def.withRegion(settingModel.getRegion())).thenReturn(withGrp);
        when(withGrp.withNewResourceGroup(settingModel.getResourceGroup())).thenReturn(with);

        try {
            azureWebAppMvpModel.createWebApp(settingModel);
        }catch (Exception e) {
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

        try{
            azureWebAppMvpModel.createWebApp(settingModel);
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

        try{
            azureWebAppMvpModel.createWebAppOnLinux(model);
        } catch(Exception e) {
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
        }catch(Exception e) {
            printException(e);
        }

        verify(appSrvPlans, times(1)).list();
    }

    @Test
    public void testListWebAppsBySubscriptionId() {
        List<WebApp> storedList = new PagedList<WebApp>() {
            @Override
            public Page<WebApp> nextPage(String nextPageLink) throws RestException, IOException {
                return null;
            }
        };

        WebApp app1 = mock(WebApp.class); when(app1.operatingSystem()).thenReturn(OperatingSystem.LINUX);
        WebApp app2 = mock(WebApp.class); when(app2.operatingSystem()).thenReturn(OperatingSystem.LINUX);
        WebApp app3 = mock(WebApp.class); when(app3.operatingSystem()).thenReturn(OperatingSystem.WINDOWS);
        storedList.add(app1);
        storedList.add(app2);
        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) storedList);

        List<ResourceEx<WebApp>> rstList = azureWebAppMvpModel.listWebAppsBySubscriptionId(MOCK_SUBSCRIPTION, false);
        verify(webAppsMock, times(1)).list();
        assertEquals(0, rstList.size());
        reset(webAppsMock);

        storedList.add(app3);
        rstList = azureWebAppMvpModel.listWebAppsBySubscriptionId(MOCK_SUBSCRIPTION, false);
        verify(webAppsMock, times(0)).list();
        assertEquals(0, rstList.size());
        reset(webAppsMock);

        when(webAppsMock.list()).thenReturn((PagedList<WebApp>) storedList);
        rstList = azureWebAppMvpModel.listWebAppsBySubscriptionId(MOCK_SUBSCRIPTION, true);
        verify(webAppsMock, times(1)).list();
        assertEquals(1, rstList.size());
        reset(webAppsMock);
    }

    @Test
    public void testListWebApps() {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Subscription sub1 = mock(Subscription.class); when(sub1.subscriptionId()).thenReturn("1");
        Subscription sub2 = mock(Subscription.class); when(sub2.subscriptionId()).thenReturn("2");
        Subscription sub3 = mock(Subscription.class); when(sub3.subscriptionId()).thenReturn("3");
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        try {
            when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);
            when(webAppsMock.list()).thenReturn(new PagedList<WebApp>() {
                @Override
                public Page<WebApp> nextPage(String nextPageLink) throws RestException, IOException {
                    return null;
                }
            });
        }catch (Exception e){
            printException(e);
        }

        mockWebAppModel.listWebApps(false);
        verify(mockWebAppModel, times(0)).listWebAppsBySubscriptionId(anyString(), eq(false));

        subscriptions.add(sub1);
        subscriptions.add(sub2);
        subscriptions.add(sub3);
        mockWebAppModel.listWebApps(false);
        verify(mockWebAppModel, times(3)).listWebAppsBySubscriptionId(anyString(), eq(false));
        reset(mockWebAppModel);

        mockWebAppModel.listWebApps(true);
        verify(mockWebAppModel, times(3)).listWebAppsBySubscriptionId(anyString(), eq(true));
    }

    @Test
    public void testListWebAppsOnLinuxBySubscriptionId() {
        List< SiteInner > storedList = new PagedList<SiteInner>() {
            @Override
            public Page<SiteInner> nextPage(String nextPageLink) throws RestException, IOException {
                return null;
            }
        };

        SiteInner app1 = mock(SiteInner.class); when(app1.kind()).thenReturn("app");
        SiteInner app2 = mock(SiteInner.class); when(app2.kind()).thenReturn("app");
        SiteInner app3 = mock(SiteInner.class); when(app3.kind()).thenReturn("app,linux");
        storedList.add(app1);
        storedList.add(app2);
        WebAppsInner inner = mock(WebAppsInner.class);
        when(webAppsMock.inner()).thenReturn(inner);
        when(inner.list()).thenReturn((PagedList<SiteInner>) storedList);

        List<ResourceEx<SiteInner>> rstList = azureWebAppMvpModel.listWebAppsOnLinuxBySubscriptionId(MOCK_SUBSCRIPTION, false);
        verify(inner, times(1)).list();
        assertEquals(0, rstList.size());
        reset(inner);

        storedList.add(app3);
        rstList = azureWebAppMvpModel.listWebAppsOnLinuxBySubscriptionId(MOCK_SUBSCRIPTION, false);
        verify(inner, times(0)).list();
        assertEquals(0, rstList.size());
        reset(inner);

        when(inner.list()).thenReturn((PagedList<SiteInner>) storedList);
        rstList = azureWebAppMvpModel.listWebAppsOnLinuxBySubscriptionId(MOCK_SUBSCRIPTION, true);
        verify(inner, times(1)).list();
        assertEquals(1, rstList.size());
        reset(inner);
    }

    @Test
    public void testListAllWebAppsOnLinux() {
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Subscription sub1 = mock(Subscription.class); when(sub1.subscriptionId()).thenReturn("1");
        Subscription sub2 = mock(Subscription.class); when(sub2.subscriptionId()).thenReturn("2");
        Subscription sub3 = mock(Subscription.class); when(sub3.subscriptionId()).thenReturn("3");
        when(mvpModel.getSelectedSubscriptions()).thenReturn(subscriptions);

        AzureWebAppMvpModel mockWebAppModel = spy(azureWebAppMvpModel);
        try {
            when(authMethodManagerMock.getAzureClient(anyString())).thenReturn(azureMock);
            WebAppsInner inner = mock(WebAppsInner.class);
            when(webAppsMock.inner()).thenReturn(inner);
            when(inner.list()).thenReturn(new PagedList<SiteInner>() {
                @Override
                public Page<SiteInner> nextPage(String nextPageLink) throws RestException, IOException {
                    return null;
                }
            });
        }catch (Exception e){
            printException(e);
        }

        mockWebAppModel.listAllWebAppsOnLinux(false);
        verify(mockWebAppModel, times(0)).listWebAppsOnLinuxBySubscriptionId(anyString(), eq(false));

        subscriptions.add(sub1);
        subscriptions.add(sub2);
        subscriptions.add(sub3);
        mockWebAppModel.listAllWebAppsOnLinux(false);
        verify(mockWebAppModel, times(3)).listWebAppsOnLinuxBySubscriptionId(anyString(), eq(false));
        reset(mockWebAppModel);

        mockWebAppModel.listAllWebAppsOnLinux(true);
        verify(mockWebAppModel, times(3)).listWebAppsOnLinuxBySubscriptionId(anyString(), eq(true));
    }

    @Test
    public void testListWebContainers() {
        // TODO: will contain all the container types from WebContainer?
        List<WebAppUtils.WebContainerMod> containers = azureWebAppMvpModel.listWebContainers();
        assertEquals(5, containers.size());
    }

    @Test
    public void testListJdks() {
        assertEquals(JavaVersion.values().size() - 1, azureWebAppMvpModel.listJdks().size());
    }

    private void printException(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        System.out.println(sStackTrace);
    }
}
