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

package com.microsoft.intellij.runner.webapp;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.AzureConfigurableProvider;
import com.microsoft.intellij.runner.webapp.webappconfig.IntelliJWebAppSettingModel;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppConfiguration;
import com.microsoft.intellij.tooling.IntegrationTestBase;
import com.microsoft.intellij.tooling.TestSchedulerProvider;
import com.microsoft.intellij.runner.webapp.webappconfig.WebAppRunState;
import com.microsoft.rest.RestClient;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.UIHelper;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PowerMockIgnore("javax.net.ssl.*")
@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthMethodManager.class, AzureManager.class, SubscriptionManager.class, DefaultLoader.class,
    TextConsoleBuilderFactory.class, WebAppUtils.class, FTPClient.class})

@Ignore
public class WebAppIntegrationTest extends IntegrationTestBase {

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

    @Mock
    private Project project;

    @Mock
    private Executor executor;

    @Mock
    private ProgramRunner programRunner;

    @Mock
    private TextConsoleBuilderFactory textConsoleBuilderFactory;

    @Mock
    private ConsoleView consoleView;

    @Mock
    private FTPClient ftpClient;

    @Mock
    private ConfigurationFactory factory;

    private TestSchedulerProvider testSchedulerProvider = new TestSchedulerProvider();
    private String defaultSubscription;
    private URL targetFolder = WebAppIntegrationTest.class.getClassLoader().getResource(".");

    @Before
    public void setUp() throws Exception {
        setUpStep();
        SchedulerProviderFactory.getInstance().init(testSchedulerProvider);
        PowerMockito.mockStatic(TextConsoleBuilderFactory.class);
        if (IS_MOCKED) {
            PowerMockito.mockStatic(WebAppUtils.class);
            when(WebAppUtils.getFtpConnection(any())).thenReturn(ftpClient);
            whenNew(FTPClient.class).withNoArguments().thenReturn(ftpClient);
            when(ftpClient.getReplyCode()).thenReturn(201);
            when(ftpClient.login(anyString(), anyString())).thenReturn(true);
            when(ftpClient.storeFile(any(), any())).thenReturn(true);
        }
        TextConsoleBuilder textConsoleBuilder = mock(TextConsoleBuilder.class);
        when(TextConsoleBuilderFactory.getInstance()).thenReturn(textConsoleBuilderFactory);
        when(textConsoleBuilderFactory.createBuilder(project)).thenReturn(textConsoleBuilder);
        when(textConsoleBuilder.getConsole()).thenReturn(consoleView);
    }

    private WebAppRunState getWebAppRunState(WebAppConfig webAppConfig) {
        WebAppConfiguration configuration = new WebAppConfiguration(project, factory, null);
        initialSetting(configuration.getModel(), webAppConfig);
        return new WebAppRunState(project, configuration);
    }

    @Test
    public void testNewWebApp() throws Exception {
        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp1", "test.war", "Tomcat 7.0", "barneytestrg",
            "barneytestplan1", "eastus", "Basic_B1", JavaVersion.JAVA_8_NEWEST, true, true, true, true,
            "barneytestrg");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();
        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
    }

    @Test
    public void testNewWebAppWithExistedRgAndASP() throws Exception {
        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp2", "test.war", "Tomcat 8.5", "barneytestrg",
            "barneytestplan1", "eastus", "Basic_B1", JavaVersion.JAVA_7_NEWEST, true, true, false, false,
            "barneytestrg");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();
        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
    }

    @Test
    public void testNewWebAppWithExistedRg() throws Exception {

        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp3", "test.war", "Jetty 9.3", "barneytestrg",
            "barneytestplan2", "eastus", "Basic_B1", JavaVersion.JAVA_ZULU_1_8_0_92, false, true, false, true,
            "barneytestrg");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();
        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
    }

    @Test
    public void testNewWebAppWithExistedASP() throws Exception {

        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp4", "test.war", "Jetty 9.1", "barneytestrg2",
            "barneytestplan2", "eastus", "Basic_B1", JavaVersion.JAVA_ZULU_1_8_0_102, false, true, true, false,
            "barneytestrg");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();
        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
    }

    @Test
    public void testNewWebAppWithJar() throws Exception {
        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp6", "test.jar", "Tomcat 8.5", "barneytestrg3",
            "barneytestplan3", "eastus", "Basic_B1", JavaVersion.JAVA_8_NEWEST, true, true, true, true,
            "barneytestrg3");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();
        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
    }

    @Test
    public void testExistedWebApp() throws Exception {

        WebAppConfig webAppConfig = new WebAppConfig("barneytestapp4", "test.war", "Jetty 9.1", "barneytestrg2",
            "barneytestplan2", "eastus", "Basic_B1", JavaVersion.JAVA_ZULU_1_8_0_102, true, false, true, false,
            "barneytestrg2");
        WebAppRunState runState = getWebAppRunState(webAppConfig);
        WebApp existedApp = getWebApp(webAppConfig.webAppName);
        runState.execute(executor, programRunner);
        testSchedulerProvider.triggerActions();

        WebApp selectedApp = getWebApp(webAppConfig.webAppName);
        assertNotNull(selectedApp);
        assertTrue(!(existedApp.lastModifiedTime().isEqual(selectedApp.lastModifiedTime())));
    }

    @After
    public void tearDown() throws Exception {
        resetTest(name.getMethodName());
    }

    private WebAppSettingModel initialSetting(WebAppSettingModel settingModel, WebAppConfig webAppConfig) {

        settingModel.setWebAppName(webAppConfig.webAppName);
        settingModel.setTargetName(webAppConfig.targetName);
        settingModel.setWebContainer(webAppConfig.webContainer);
        settingModel.setResourceGroup(webAppConfig.resourceGroup);
        settingModel.setAppServicePlanName(webAppConfig.appServicePlanName);
        settingModel.setRegion(webAppConfig.region);
        settingModel.setPricing(webAppConfig.pricing);
        settingModel.setJdkVersion(webAppConfig.jdkVersion);
        settingModel.setDeployToRoot(webAppConfig.deployToRoot);
        settingModel.setCreatingNew(webAppConfig.creatingNew);
        settingModel.setCreatingResGrp(webAppConfig.creatingResGrp);
        settingModel.setCreatingAppServicePlan(webAppConfig.creatingAppServicePlan);
        settingModel.setTargetPath(Paths.get("test/resources", webAppConfig.targetName).toString());
        settingModel.setAppServicePlanId(webAppConfig.AppServicePlanId);
        settingModel.setWebAppId(webAppConfig.webAppId);
        settingModel.setJdkVersion(JavaVersion.JAVA_8_NEWEST);
        settingModel.setSubscriptionId(defaultSubscription);
        return settingModel;
    }

    @Override
    protected void initialize(RestClient restClient, String defaultSubscription, String domain) throws Exception {
        Azure.Authenticated azureAuthed = Azure.authenticate(restClient, defaultSubscription, domain);
        azure = azureAuthed.withSubscription(defaultSubscription);
        this.defaultSubscription = defaultSubscription;

        PowerMockito.mockStatic(AuthMethodManager.class);
        when(AuthMethodManager.getInstance()).thenReturn(authMethodManagerMock);
        when(authMethodManagerMock.getAzureClient(defaultSubscription)).thenReturn(azure);

        when(authMethodManagerMock.getAzureManager()).thenReturn(azureManagerMock);
        when(azureManagerMock.getSubscriptionManager()).thenReturn(subscriptionManagerMock);
        final Map<String, Subscription> mockSidToSubscriptionMap = new HashMap<>();
        mockSidToSubscriptionMap.put(defaultSubscription, subscriptionMock);
        final Map<String, SubscriptionDetail> mockSidToSubDetailMap = new HashMap<>();
        mockSidToSubDetailMap.put(defaultSubscription, subscriptionDetailMock);

        when(subscriptionDetailMock.isSelected()).thenReturn(true);
        when(subscriptionDetailMock.getSubscriptionId()).thenReturn(defaultSubscription);
        when(subscriptionManagerMock.getSubscriptionIdToSubscriptionDetailsMap()).thenReturn(mockSidToSubDetailMap);
        when(subscriptionManagerMock.getSubscriptionIdToSubscriptionMap()).thenReturn(mockSidToSubscriptionMap);
        when(subscriptionMock.subscriptionId()).thenReturn(defaultSubscription);
        PowerMockito.mockStatic(DefaultLoader.class);
        when(DefaultLoader.getUIHelper()).thenReturn(uiHelper);
    }

    private WebApp getWebApp(String webappName) {
        List<ResourceEx<WebApp>> webApps = AzureWebAppMvpModel.getInstance().listAllWebAppsOnWindows(true);
        WebApp selectedApp = null;
        for (ResourceEx<WebApp> webApp : webApps) {
            if (webApp.getResource().name().equalsIgnoreCase(webappName)) {
                selectedApp = webApp.getResource();
            }
        }
        return selectedApp;
    }

    private class WebAppConfig {
        public WebAppConfig(String webAppName, String targetName, String webContainer, String resourceGroup,
                            String appServicePlanName, String region, String pricing, JavaVersion jdkVersion,
                            boolean deployToRoot,
                            boolean creatingNew, boolean creatingResGrp, boolean creatingAppServicePlan,
                            String appPlanRg) {

            String appPlanFormatString = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/serverfarms/%s";
            String webAppFormatString = "/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Web/sites/%s";
            this.webAppName = webAppName;
            this.targetName = targetName;
            this.webContainer = webContainer;
            this.resourceGroup = resourceGroup;
            this.appServicePlanName = appServicePlanName;
            this.region = region;
            this.pricing = pricing;
            this.jdkVersion = jdkVersion;
            this.deployToRoot = deployToRoot;
            this.creatingNew = creatingNew;
            this.creatingResGrp = creatingResGrp;
            this.creatingAppServicePlan = creatingAppServicePlan;

            this.AppServicePlanId = "";
            if (!this.creatingAppServicePlan) {
                this.AppServicePlanId = String.format(appPlanFormatString, defaultSubscription, appPlanRg,
                    this.appServicePlanName);
            }
            if (!this.creatingNew) {
                this.webAppId = String.format(webAppFormatString, defaultSubscription, this.resourceGroup,
                    this.webAppName);
            }
        }

        private String webAppName;
        private String webAppId;
        private String AppServicePlanId;
        private String targetName;
        private String webContainer;
        private String resourceGroup;
        private String appServicePlanName;
        private String region;
        private String pricing;
        private JavaVersion jdkVersion;
        private boolean deployToRoot;
        private boolean creatingNew;
        private boolean creatingResGrp;
        private boolean creatingAppServicePlan;
    }
}
