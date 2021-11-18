/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.webapp;

import com.microsoft.azure.toolkit.ide.appservice.model.MonitorConfig;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.appservice.model.DiagnosticConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class WebAppService {
    private static final WebAppService instance = new WebAppService();

    public static WebAppService getInstance() {
        return WebAppService.instance;
    }

    @AzureOperation(name = "webapp.create_detail", params = {"config.getName()"}, type = AzureOperation.Type.SERVICE)
    public IWebApp createWebApp(final WebAppConfig config) {
        final WebAppSettingModel settings = convertConfig2Settings(config);
        settings.setCreatingNew(true);
        final Map<String, String> properties = settings.getTelemetryProperties(null);
        final Operation operation = TelemetryManager.createOperation(WEBAPP, CREATE_WEBAPP);
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

    @AzureOperation(
        name = "webapp.init_config",
        type = AzureOperation.Type.TASK
    )
    public static WebAppSettingModel convertConfig2Settings(final WebAppConfig config) {
        final WebAppSettingModel settings = new WebAppSettingModel();
        settings.setSubscriptionId(config.getSubscription().getId());
        // creating if id is empty
        settings.setCreatingResGrp(config.getResourceGroup() instanceof Draft || StringUtils.isEmpty(config.getResourceGroup().getId()));
        settings.setResourceGroup(config.getResourceGroup().getName());
        settings.setWebAppName(config.getName());
        settings.setRegion(config.getRegion().getName());
        settings.saveRuntime(config.getRuntime());
        // creating if id is empty
        settings.setCreatingAppServicePlan(config.getServicePlan() instanceof Draft || StringUtils.isEmpty(config.getServicePlan().getId()));
        if (settings.isCreatingAppServicePlan()) {
            settings.setAppServicePlanName(config.getServicePlan().getName());
        } else {
            settings.setAppServicePlanId(config.getServicePlan().getId());
        }
        settings.setPricing(config.getServicePlan().getPricingTier().getSize());
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
