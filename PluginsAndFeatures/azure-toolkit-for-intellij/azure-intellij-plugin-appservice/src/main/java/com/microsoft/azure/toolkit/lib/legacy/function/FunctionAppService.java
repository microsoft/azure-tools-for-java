/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.legacy.function;

import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.FlexConsumptionConfiguration;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask.APPINSIGHTS_INSTRUMENTATION_KEY;

public class FunctionAppService {

    private static final FunctionAppService instance = new FunctionAppService();

    public static FunctionAppService getInstance() {
        return FunctionAppService.instance;
    }

    public FunctionAppConfig getFunctionAppConfigFromExistingFunction(@Nonnull final FunctionApp functionApp) {
        final String applicationInsightsKey = Optional.ofNullable(functionApp.getAppSettings()).map(m -> m.get(APPINSIGHTS_INSTRUMENTATION_KEY)).orElse(null);
        final MonitorConfig monitorConfig = MonitorConfig.builder()
                .diagnosticConfig(functionApp.getDiagnosticConfig())
                .applicationInsightsConfig(ApplicationInsightsConfig.builder().instrumentationKey(applicationInsightsKey).build())
                .build();
        final FlexConsumptionConfiguration flexConsumptionConfiguration = functionApp.getFlexConsumptionConfiguration();
        return FunctionAppConfig.builder()
                .resourceId(functionApp.getId())
                .name(functionApp.getName())
                .region(functionApp.getRegion())
                .runtime(functionApp.getRuntime())
                .resourceGroup(ResourceGroupConfig.fromResource(functionApp.getResourceGroup()))
                .subscription(functionApp.getSubscription())
                .appSettings(functionApp.getAppSettings())
                .monitorConfig(monitorConfig)
                .flexConsumptionConfiguration(flexConsumptionConfiguration)
                .servicePlan(AppServicePlanConfig.fromResource(functionApp.getAppServicePlan())).build();
    }

    public FunctionAppBase<?, ?, ?> createOrUpdateFunctionApp(final FunctionAppConfig config) {
        final CreateOrUpdateFunctionAppTask task = new CreateOrUpdateFunctionAppTask(convertToTaskConfig(config));
        final FunctionAppBase<?, ?, ?> execute = task.execute();
        if (execute instanceof AzResource.Draft) {
            ((AzResource.Draft<?, ?>) execute).reset();
        }
        return execute;
    }

    private com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig convertToTaskConfig(final FunctionAppConfig config) {
        final com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig result = new com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig();
        result.subscriptionId(config.getSubscription().getId());
        result.resourceGroup(config.getResourceGroupName());
        result.appName(config.getName());
        result.region(config.getRegion());
        result.pricingTier(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getPricingTier).orElseGet(config::getPricingTier));
        result.servicePlanName(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getName).orElseGet(config::getName));
        result.servicePlanResourceGroup(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getResourceGroupName).orElseGet(config::getResourceGroupName));
        result.runtime(convertToRuntimeConfig(config.getRuntime()));
        result.appSettings(config.getAppSettings());
        result.appSettingsToRemove(getAppSettingsToRemove(config));
        final ApplicationInsightsConfig applicationInsightsConfig =
                Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getApplicationInsightsConfig).orElse(null);
        result.disableAppInsights(applicationInsightsConfig == null);
        Optional.ofNullable(applicationInsightsConfig).ifPresent(insights -> {
            result.appInsightsInstance(insights.getName());
            result.appInsightsKey(insights.getInstrumentationKey());
            result.workspaceConfig(insights.getWorkspaceConfig());
        });
        Optional.ofNullable(config.getDeploymentSlot()).ifPresent(slot -> {
            result.deploymentSlotName(slot.getName());
            result.deploymentSlotConfigurationSource(slot.getConfigurationSource());
        });
        Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getDiagnosticConfig).ifPresent(result::diagnosticConfig);
        // currently ide did not support set flex consumption options, so use default values
        Optional.ofNullable(config.getFlexConsumptionConfiguration()).ifPresent(result::flexConsumptionConfiguration);
        return result;
    }

    private Set<String> getAppSettingsToRemove(FunctionAppConfig config) {
        if (StringUtils.isBlank(config.getResourceId())) {
            // do not remove app settings for new function app
            return Collections.emptySet();
        }
        final Map<String, String> applicationSettings = config.getAppSettings();
        final FunctionAppBase<?, ?, ?> target = config.getDeploymentSlot() == null ? Azure.az(AzureFunctions.class).functionApp(config.getResourceId())
                : Objects.requireNonNull(Azure.az(AzureFunctions.class).functionApp(config.getResourceId())).slots().get(config.getDeploymentSlot().getName(), null);
        return target == null || !target.exists() ? Collections.emptySet() : Optional.ofNullable(target.getAppSettings())
                .map(settings -> settings.keySet().stream().filter(key -> !applicationSettings.containsKey(key)).collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
    }

    private RuntimeConfig convertToRuntimeConfig(Runtime runtime) {
        return new RuntimeConfig().os(runtime.getOperatingSystem()).webContainer(runtime.getWebContainer()).javaVersion(runtime.getJavaVersion());
    }

    public void deployFunctionApp(final FunctionAppBase<?, ?, ?> functionApp, final File stagingFolder) {
        final DeployFunctionAppTask deployFunctionAppTask = new DeployFunctionAppTask(functionApp, stagingFolder, null);
        deployFunctionAppTask.execute();
    }
}
