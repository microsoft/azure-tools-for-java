/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.legacy.function;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.task.GetOrCreateApplicationInsightsTask;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDraft;
import com.microsoft.azure.toolkit.lib.appservice.model.FunctionDeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.zip.ZipUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

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

    public FunctionAppConfig getFunctionAppConfigFromExistingFunction(@Nonnull final FunctionApp functionApp) {
        return FunctionAppConfig.builder()
                .resourceId(functionApp.getId())
                .name(functionApp.getName())
                .region(functionApp.getRegion())
                .resourceGroup(ResourceGroupConfig.fromResource(functionApp.getResourceGroup()))
                .subscription(functionApp.getSubscription())
                .servicePlan(AppServicePlanConfig.fromResource(functionApp.getAppServicePlan())).build();
    }

    public FunctionApp createFunctionApp(final FunctionAppConfig config) {
        OperationContext.action().setTelemetryProperty(CREATE_NEW_FUNCTION_APP, String.valueOf(true));
        final ResourceGroup resourceGroup = getOrCreateResourceGroup(config);
        final AppServicePlan appServicePlan = getOrCreateAppServicePlan(config);
        final Map<String, String> appSettings = getAppSettings(config);
        // get/create ai instances only if user didn't specify ai connection string in app settings
        OperationContext.action().setTelemetryProperty(DISABLE_APP_INSIGHTS, String.valueOf(config.getMonitorConfig().getApplicationInsightsConfig() == null));
        bindApplicationInsights(appSettings, config);
        final FunctionAppDraft draft = Azure.az(AzureFunctions.class).functionApps(config.getSubscription().getId())
            .create(config.getName(), resourceGroup.getName());
        draft.setAppServicePlan(appServicePlan);
        draft.setRuntime(config.getRuntime());
        draft.setAppSettings(appSettings);
        draft.setDiagnosticConfig(config.getMonitorConfig().getDiagnosticConfig());
        return draft.createIfNotExist();
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
        final String rgName = config.getResourceGroup().getName();
        return Azure.az(AzureResources.class).groups(config.getSubscription().getId())
            .createResourceGroupIfNotExist(rgName, config.getRegion());
    }

    private AppServicePlan getOrCreateAppServicePlan(final FunctionAppConfig config) {
        final String servicePlanName = config.getServicePlan().getName();
        final String servicePlanGroup = StringUtils.firstNonBlank(config.getServicePlan().getResourceGroupName(), config.getResourceGroup().getName());
        final AppServicePlan appServicePlan = Azure.az(AzureAppService.class).plans(config.getSubscription().getId())
            .getOrDraft(servicePlanName, servicePlanGroup);
        if (!appServicePlan.exists()) {
            OperationContext.action().setTelemetryProperty(CREATE_NEW_APP_SERVICE_PLAN, String.valueOf(true));
            final AppServicePlanDraft draft = (AppServicePlanDraft) appServicePlan;
            draft.setRegion(config.getRegion());
            draft.setPricingTier(config.getServicePlan().getPricingTier());
            draft.setOperatingSystem(config.getRuntime().getOperatingSystem());
            return draft.createIfNotExist();
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
            final ApplicationInsight applicationInsightsComponent = getOrCreateApplicationInsights(config);
            instrumentationKey = applicationInsightsComponent == null ? null : applicationInsightsComponent.getInstrumentationKey();
        }
        appSettings.put(APPINSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
    }

    @Nullable
    private ApplicationInsight getOrCreateApplicationInsights(final FunctionAppConfig config) {
        final String subscriptionId = config.getSubscription().getId();
        final String resourceGroup = config.getResourceGroup().getName();
        final String name = config.getMonitorConfig().getApplicationInsightsConfig().getName();
        final Region region = config.getRegion();
        return new GetOrCreateApplicationInsightsTask(subscriptionId, resourceGroup, region, name).execute();
    }

    public void deployFunctionApp(final FunctionApp functionApp, final File stagingFolder) throws IOException {
        AzureMessager.getMessager().info(DEPLOY_START);
        final FunctionDeployType deployType = getDeployType(functionApp);
        OperationContext.action().setTelemetryProperty(DEPLOYMENT_TYPE, deployType.name());
        functionApp.deploy(packageStagingDirectory(stagingFolder), deployType);
        if (!StringUtils.equalsIgnoreCase(functionApp.getStatus(), RUNNING)) {
            functionApp.start();
        }
        final String resourceUrl = String.format(PORTAL_URL_PATTERN, Azure.az(AzureAccount.class).account().getPortalUrl(), functionApp.id());
        AzureMessager.getMessager().info(String.format(DEPLOY_FINISH, resourceUrl));
    }

    private FunctionDeployType getDeployType(final FunctionApp functionApp) {
        if (functionApp.getRuntime().getOperatingSystem() == OperatingSystem.WINDOWS) {
            return FunctionDeployType.RUN_FROM_ZIP;
        }
        final PricingTier pricingTier = functionApp.getAppServicePlan().getPricingTier();
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
