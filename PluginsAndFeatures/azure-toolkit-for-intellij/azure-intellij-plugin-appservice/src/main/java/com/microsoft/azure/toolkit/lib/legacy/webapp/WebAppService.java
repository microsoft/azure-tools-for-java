/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.webapp;

import com.microsoft.azure.toolkit.ide.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.*;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebAppService {
    private static final WebAppService instance = new WebAppService();

    public static WebAppService getInstance() {
        return WebAppService.instance;
    }

    @AzureOperation(name = "user/webapp.create_app.app", params = {"config.getName()"})
    public WebApp createWebApp(final WebAppConfig config) {
        final WebAppSettingModel settings = convertConfig2Settings(config);
        settings.setCreatingNew(true);
        final Map<String, String> properties = settings.getTelemetryProperties(null);
        final Operation operation = TelemetryManager.createOperation("webapp", "create-webapp");
        try {
            operation.start();
            operation.trackProperties(properties);
            return AzureWebAppMvpModel.getInstance().createWebAppFromSettingModel(settings);
        } catch (final RuntimeException e) {
            EventUtil.logError(operation, ErrorType.userError, e, properties, null);
            throw e;
        } finally {
            operation.complete();
        }
    }

    public static com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig convertToTaskConfig(final WebAppConfig config) {
        final com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig result = new com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig();
        result.subscriptionId(config.getSubscription().getId());
        result.resourceGroup(config.getResourceGroupName());
        result.appName(config.getName());
        result.region(config.getRegion());
        result.pricingTier(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::getPricingTier).orElseGet(config::getPricingTier));
        result.servicePlanName(config.getServicePlan().getName());
        result.servicePlanResourceGroup(StringUtils.firstNonBlank(config.getServicePlan().getResourceGroupName(), config.getResourceGroup().getName()));
        result.runtime(convertToRuntimeConfig(config.getRuntime()));
        result.appSettings(config.getAppSettings());
        final ApplicationInsightsConfig applicationInsightsConfig =
                Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getApplicationInsightsConfig).orElse(null);
        Optional.ofNullable(config.getDeploymentSlot()).ifPresent(slot -> {
            result.deploymentSlotName(slot.getName());
            result.deploymentSlotConfigurationSource(slot.getConfigurationSource());
        });
        Optional.ofNullable(config.getMonitorConfig()).map(MonitorConfig::getDiagnosticConfig).ifPresent(result::diagnosticConfig);
        return result;
    }

    private static RuntimeConfig convertToRuntimeConfig(Runtime runtime) {
        return new RuntimeConfig().os(runtime.getOperatingSystem()).webContainer(runtime.getWebContainer()).javaVersion(runtime.getJavaVersion());
    }

    public static WebAppSettingModel convertConfig2Settings(final WebAppConfig config) {
        final WebAppSettingModel settings = new WebAppSettingModel();
        settings.setSubscriptionId(config.getSubscription().getId());
        // creating if id is empty
        final ResourceGroup rg = config.getResourceGroup().toResource();
        settings.setResourceGroup(rg.getName());
        settings.setWebAppName(config.getName());
        settings.setRegion(config.getRegion().getName());
        settings.saveRuntime(config.getRuntime());
        // creating if id is empty
        final AppServicePlan plan = config.getServicePlan().toResource();
        settings.setCreatingAppServicePlan(plan.isDraftForCreating() || StringUtils.isEmpty(plan.getId()));
        settings.setAppServicePlanName(plan.getName());
        settings.setAppServicePlanResourceGroupName(plan.getResourceGroupName());
        settings.setPricing(plan.getPricingTier().getSize());
        final MonitorConfig monitorConfig = config.getMonitorConfig();
        if (monitorConfig != null) {
            final DiagnosticConfig diagnosticConfig = monitorConfig.getDiagnosticConfig();
            settings.setEnableApplicationLog(diagnosticConfig.isEnableApplicationLog());
            settings.setApplicationLogLevel(diagnosticConfig.getApplicationLogLevel() == null ? null :
                    diagnosticConfig.getApplicationLogLevel().getValue());
            settings.setEnableWebServerLogging(diagnosticConfig.isEnableWebServerLogging());
            settings.setWebServerLogQuota(diagnosticConfig.getWebServerLogQuota());
            settings.setWebServerRetentionPeriod(diagnosticConfig.getWebServerRetentionPeriod());
            settings.setEnableDetailedErrorMessage(diagnosticConfig.isEnableDetailedErrorMessage());
            settings.setEnableFailedRequestTracing(diagnosticConfig.isEnableFailedRequestTracing());
        }
        settings.setTargetName(config.getApplication() == null ? null : config.getApplication().toFile().getName());
        settings.setTargetPath(config.getApplication() == null ? null : config.getApplication().toString());
        return settings;
    }

    public String getRuntimeDisplayName(@Nonnull final Runtime runtime) {
        if (runtime.getOperatingSystem() == OperatingSystem.DOCKER) {
            return "Docker";
        }
        final String os = runtime.getOperatingSystem().getValue();
        final String javaVersion = Objects.equals(runtime.getJavaVersion(), JavaVersion.OFF) ?
                null : String.format("Java %s", runtime.getJavaVersion().getValue());
        final String webContainer = Objects.equals(runtime.getWebContainer(), WebContainer.JAVA_OFF) ?
                null : runtime.getWebContainer().getValue();
        return Stream.of(os, javaVersion, webContainer)
                     .filter(StringUtils::isNotEmpty)
                     .map(StringUtils::capitalize).collect(Collectors.joining("-"));
    }
}
