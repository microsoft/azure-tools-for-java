/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.LogLevel;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDeployModel {
    private String appSettingsKey;
    private String deploymentStagingDirectoryPath;
    private String moduleName;

    private FunctionAppConfig functionAppConfig = FunctionAppConfig.builder().build();

    public FunctionDeployModel(DeprecatedDeployModel deprecatedDeployModel) {
        this.moduleName = deprecatedDeployModel.moduleName;
        this.appSettingsKey = deprecatedDeployModel.appSettingsKey;
        this.deploymentStagingDirectoryPath = deprecatedDeployModel.deploymentStagingDirectoryPath;
        final Subscription subscription = Subscription.builder().id(deprecatedDeployModel.subscription).build();
        final PricingTier pricingTier = PricingTier.fromString(deprecatedDeployModel.pricingTier);
        final OperatingSystem operatingSystem = OperatingSystem.fromString(deprecatedDeployModel.os);
        final JavaVersion javaVersion = JavaVersion.fromString(deprecatedDeployModel.javaVersion);
        final Runtime runtime = Runtime.getRuntime(operatingSystem, WebContainer.JAVA_OFF, javaVersion);
        final ApplicationInsightsConfig insightsConfig = ApplicationInsightsConfig.builder()
                .name(deprecatedDeployModel.insightsName)
                .instrumentationKey(deprecatedDeployModel.instrumentationKey).build();
        final DiagnosticConfig diagnosticConfig = DiagnosticConfig.builder()
                .enableApplicationLog(deprecatedDeployModel.enableApplicationLog)
                .applicationLogLevel(deprecatedDeployModel.applicationLogLevel).build();
        final MonitorConfig monitorConfig = MonitorConfig.builder().applicationInsightsConfig(insightsConfig).diagnosticConfig(diagnosticConfig).build();
        final ResourceGroup resourceGroup = ResourceGroup.builder().name(deprecatedDeployModel.resourceGroup).region(deprecatedDeployModel.region).build();
        final AppServicePlanEntity appServicePlan = AppServicePlanEntity.builder()
                .name(deprecatedDeployModel.appServicePlanName)
                .resourceGroup(deprecatedDeployModel.resourceGroup)
                .region(deprecatedDeployModel.region)
                .pricingTier(pricingTier).build();
        this.functionAppConfig = FunctionAppConfig.builder()
                .resourceId(deprecatedDeployModel.functionId)
                .subscription(subscription)
                .resourceGroup(resourceGroup)
                .name(deprecatedDeployModel.appName)
                .servicePlan(appServicePlan)
                .pricingTier(pricingTier)
                .runtime(runtime)
                .monitorConfig(monitorConfig)
                .appSettings(FunctionUtils.loadAppSettingsFromSecurityStorage(appSettingsKey))
                .build();
    }

    public Map<String, String> getTelemetryProperties() {
        return Optional.ofNullable(functionAppConfig).map(FunctionAppConfig::getTelemetryProperties).orElse(Collections.emptyMap());
    }

    // for migrate old configuration to new resource config
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class DeprecatedDeployModel {
        private boolean isNewResource;
        private String functionId;
        private boolean enableApplicationLog;
        private LogLevel applicationLogLevel;
        private String subscription;
        private String resourceGroup;
        private String appName;
        private String region;
        private String pricingTier;
        private String appServicePlanResourceGroup;
        private String appServicePlanName;
        private String deploymentStagingDirectoryPath;
        private String deployment;
        private Map<String, String> appSettings = new HashMap<>();
        private String appSettingsKey;
        private String moduleName;
        private String insightsName;
        private String instrumentationKey;
        private String os;
        private String javaVersion;
    }
}
