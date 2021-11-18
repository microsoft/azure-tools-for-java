/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.function;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.azure.toolkit.ide.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightsEntity;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.azure.toolkit.lib.resource.AzureGroup;
import com.microsoft.azuretools.sdkmanage.IdentityAzureManager;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionAppService {
    private static final String CREATE_NEW_FUNCTION_APP = "isCreateNewFunctionApp";
    private static final String CREATE_NEW_RESOURCE_GROUP = "createNewResourceGroup";
    private static final String CREATE_NEW_APP_SERVICE_PLAN = "createNewAppServicePlan";
    private static final String DEPLOYMENT_TYPE = "deploymentType";
    private static final String DISABLE_APP_INSIGHTS = "disableAppInsights";

    private static final String CREATE_RESOURCE_GROUP = "Creating resource group %s in region %s...";
    private static final String CREATE_RESOURCE_GROUP_DONE = "Successfully created resource group %s.";
    private static final String CREATE_APP_SERVICE_PLAN = "Creating app service plan...";
    private static final String CREATE_APP_SERVICE_DONE = "Successfully created app service plan %s.";
    private static final String CREATE_FUNCTION_APP = "Creating function app %s...";
    private static final String CREATE_FUNCTION_APP_DONE = "Successfully created function app %s.";
    private static final String APPINSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";
    private static final String APPLICATION_INSIGHTS_CREATE_START = "Creating application insights...";
    private static final String APPLICATION_INSIGHTS_CREATED = "Successfully created the application insights %s " +
            "for this Function App. You can visit %s/#@/resource%s/overview to view your " +
            "Application Insights component.";
    private static final String APPLICATION_INSIGHTS_CREATE_FAILED = "Unable to create the Application Insights " +
            "for the Function App due to error %s. Please use the Azure Portal to manually create and configure the " +
            "Application Insights if needed.";
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String DEPLOY_START = "Starting deployment...";
    private static final String DEPLOY_FINISH = "Deployment done, you may access your resource through %s";
    private static final String RUNNING = "Running";
    private static final String PORTAL_URL_PATTERN = "%s/#@/resource%s";
    private static final String LOCAL_SETTINGS_FILE = "local.settings.json";

    private static final FunctionAppService instance = new FunctionAppService();

    public static FunctionAppService getInstance() {
        return FunctionAppService.instance;
    }

    public FunctionAppConfig getFunctionAppConfigFromExistingFunction(@Nonnull final IFunctionApp functionApp) {
        return FunctionAppConfig.builder()
                .resourceId(functionApp.id())
                .name(functionApp.name())
                .region(functionApp.entity().getRegion())
                .resourceGroup(ResourceGroup.builder().name(functionApp.resourceGroup()).build())
                .subscription(Subscription.builder().id(functionApp.subscriptionId()).build())
                .servicePlan(AppServicePlanEntity.builder().id(functionApp.entity().getAppServicePlanId()).build()).build();
    }

    public IFunctionApp createFunctionApp(final FunctionAppConfig config) {
        AzureTelemetry.getActionContext().setProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(config);
        final IAppServicePlan appServicePlan = getOrCreateAppServicePlan(config);
        AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP, config.getName()));
        final Map<String, String> appSettings = getAppSettings(config);
        // get/create ai instances only if user didn't specify ai connection string in app settings
        AzureTelemetry.getActionContext().setProperty(DISABLE_APP_INSIGHTS, String.valueOf(config.getMonitorConfig().getApplicationInsightsConfig() == null));
        bindApplicationInsights(appSettings, config);
        final IFunctionApp result = Azure.az(AzureAppService.class).subscription(config.getSubscription())
                .functionApp(resourceGroup.getName(), config.getName()).create()
                .withName(config.getName())
                .withResourceGroup(resourceGroup.getName())
                .withPlan(appServicePlan.id())
                .withRuntime(config.getRuntime())
                .withAppSettings(appSettings)
                .withDiagnosticConfig(config.getMonitorConfig().getDiagnosticConfig())
                .commit();
        AzureMessager.getMessager().info(String.format(CREATE_FUNCTION_APP_DONE, result.name()));
        return result;
    }

    private Map<String, String> getAppSettings(final FunctionAppConfig config) {
        final Map<String, String> settings = config.getAppSettings();
        setDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, message("function.hint.setFunctionWorker"),
                FUNCTIONS_WORKER_RUNTIME_VALUE, message("function.hint.changeFunctionWorker"));
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, message("function.hint.setFunctionVersion"),
                FUNCTIONS_EXTENSION_VERSION_VALUE, null);
        return settings;
    }

    private void setDefaultAppSetting(final Map<String, String> result, String settingName, String settingIsEmptyMessage,
                                      String defaultValue, String warningMessage) {
        final String setting = result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            AzureMessager.getMessager().info(settingIsEmptyMessage);
            result.put(settingName, defaultValue);
            return;
        }
        // Show warning message when user set a different value
        if (!StringUtils.equalsIgnoreCase(setting, defaultValue) && StringUtils.isNotEmpty(warningMessage)) {
            AzureMessager.getMessager().warning(warningMessage);
        }
    }

    private ResourceGroup getOrCreateResourceGroup(final FunctionAppConfig config) {
        try {
            return Azure.az(AzureGroup.class).subscription(config.getSubscription()).getByName(config.getResourceGroup().getName());
        } catch (final ManagementException e) {
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP, config.getResourceGroup().getName(), config.getRegion().getName()));
            AzureTelemetry.getActionContext().setProperty(CREATE_NEW_RESOURCE_GROUP, String.valueOf(true));
            final ResourceGroup result = Azure.az(AzureGroup.class).subscription(config.getSubscription())
                    .create(config.getResourceGroup().getName(), config.getRegion().getName());
            AzureMessager.getMessager().info(String.format(CREATE_RESOURCE_GROUP_DONE, result.getName()));
            return result;
        }
    }

    private IAppServicePlan getOrCreateAppServicePlan(final FunctionAppConfig config) {
        final String servicePlanName = config.getServicePlan().getName();
        final String servicePlanGroup = StringUtils.firstNonBlank(config.getServicePlan().getResourceGroup(), config.getResourceGroup().getName());
        final IAppServicePlan appServicePlan = Azure.az(AzureAppService.class).subscription(config.getSubscription().getId())
                .appServicePlan(servicePlanGroup, servicePlanName);
        if (!appServicePlan.exists()) {
            AzureMessager.getMessager().info(CREATE_APP_SERVICE_PLAN);
            AzureTelemetry.getActionContext().setProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            appServicePlan.create()
                    .withName(servicePlanName)
                    .withResourceGroup(servicePlanGroup)
                    .withRegion(config.getRegion())
                    .withPricingTier(config.getServicePlan().getPricingTier())
                    .withOperatingSystem(config.getRuntime().getOperatingSystem())
                    .commit();
            AzureMessager.getMessager().info(String.format(CREATE_APP_SERVICE_DONE, appServicePlan.name()));
        }
        return appServicePlan;
    }

    private void bindApplicationInsights(final Map<? super String, ? super String> appSettings, final FunctionAppConfig config) {
        // Skip app insights creation when user specify ai connection string in app settings or disable ai in configuration
        if (appSettings.containsKey(APPINSIGHTS_INSTRUMENTATION_KEY) || config.getMonitorConfig().getApplicationInsightsConfig() == null) {
            return;
        }
        String instrumentationKey = config.getMonitorConfig().getApplicationInsightsConfig().getInstrumentationKey();
        if (StringUtils.isEmpty(instrumentationKey)) {
            final ApplicationInsightsEntity applicationInsightsComponent = getOrCreateApplicationInsights(config);
            instrumentationKey = applicationInsightsComponent == null ? null : applicationInsightsComponent.getInstrumentationKey();
        }
        appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
    }

    @Nullable
    private ApplicationInsightsEntity getOrCreateApplicationInsights(final FunctionAppConfig config) {
        final ApplicationInsightsConfig insightsConfig = config.getMonitorConfig().getApplicationInsightsConfig();
        try {
            return Azure.az(ApplicationInsights.class).subscription(config.getSubscription())
                    .get(config.getResourceGroup().getName(), insightsConfig.getName());
        } catch (final ManagementException e) {
            return createApplicationInsights(config);
        }
    }

    @Nullable
    private ApplicationInsightsEntity createApplicationInsights(final FunctionAppConfig config) {
        try {
            AzureMessager.getMessager().info(APPLICATION_INSIGHTS_CREATE_START);
            final AzureEnvironment environment = Azure.az(AzureAccount.class).account().getEnvironment();
            final ApplicationInsightsEntity resource = Azure.az(ApplicationInsights.class).create(config.getSubscription().getId(),
                    config.getResourceGroup().getName(), config.getRegion(), config.getMonitorConfig().getApplicationInsightsConfig().getName());
            AzureMessager.getMessager().info(String.format(APPLICATION_INSIGHTS_CREATED,
                    resource.getName(), IdentityAzureManager.getInstance().getPortalUrl(), resource.getId()));
            return resource;
        } catch (final Exception e) {
            AzureMessager.getMessager().warning(String.format(APPLICATION_INSIGHTS_CREATE_FAILED, e.getMessage()));
            return null;
        }
    }

    public void deployFunctionApp(final IFunctionApp functionApp, final File stagingFolder) throws IOException {
        AzureMessager.getMessager().info(DEPLOY_START);
        final FunctionDeployType deployType = getDeployType(functionApp);
        AzureTelemetry.getActionContext().setProperty(DEPLOYMENT_TYPE, deployType.name());
        functionApp.deploy(packageStagingDirectory(stagingFolder), deployType);
        if (!StringUtils.equalsIgnoreCase(functionApp.state(), RUNNING)) {
            functionApp.start();
        }
        final String resourceUrl = String.format(PORTAL_URL_PATTERN, IdentityAzureManager.getInstance().getPortalUrl(), functionApp.id());
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, resourceUrl));
    }

    private FunctionDeployType getDeployType(final IFunctionApp functionApp) {
        if (functionApp.getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
        final PricingTier pricingTier = functionApp.plan().entity().getPricingTier();
        return StringUtils.equalsAnyIgnoreCase(pricingTier.getTier(), "Dynamic", "ElasticPremium") ?
                FunctionDeployType.RUN_FROM_BLOB : FunctionDeployType.RUN_FROM_ZIP;
    }

    private File packageStagingDirectory(final File stagingFolder) throws IOException {
        final File zipFile = Files.createTempFile("azure-toolkit", ".zip").toFile();
        ZipUtil.pack(stagingFolder, zipFile);
        ZipUtil.removeEntry(zipFile, LOCAL_SETTINGS_FILE);
        return zipFile;
    }
}
