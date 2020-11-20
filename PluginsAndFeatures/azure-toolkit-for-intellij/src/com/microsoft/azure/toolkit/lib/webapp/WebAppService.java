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

package com.microsoft.azure.toolkit.lib.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.Draft;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.telemetrywrapper.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class WebAppService {
    private static final WebAppService instance = new WebAppService();

    public static WebAppService getInstance() {
        return WebAppService.instance;
    }

    public WebApp createWebApp(final WebAppConfig config) throws Exception {
        final WebAppSettingModel settings = convertConfig2Settings(config);
        settings.setCreatingNew(true);
        final Map<String, String> properties = settings.getTelemetryProperties(null);
        final Operation operation = TelemetryManager.createOperation(WEBAPP, CREATE_WEBAPP);
        try {
            operation.start();
            EventUtil.logEvent(EventType.info, operation, properties);
            return AzureWebAppMvpModel.getInstance().createWebApp(settings);
        } catch (final Exception e) {
            EventUtil.logError(operation, ErrorType.userError, e, properties, null);
            throw e;
        } finally {
            operation.complete();
        }
    }

    public static WebAppSettingModel convertConfig2Settings(final WebAppConfig config) {
        final WebAppSettingModel settings = new WebAppSettingModel();
        settings.setSubscriptionId(config.getSubscription().subscriptionId());
        // creating if id is empty
        settings.setCreatingResGrp(config.getResourceGroup() instanceof Draft || StringUtils.isEmpty(config.getResourceGroup().id()));
        settings.setResourceGroup(config.getResourceGroup().name());
        settings.setWebAppName(config.getName());
        settings.setOS(config.getPlatform().getOs());
        settings.setRegion(config.getRegion().name());
        if (settings.getOS() == OperatingSystem.LINUX) {
            settings.setStack(config.getPlatform().getStackOrWebContainer());
            settings.setVersion(config.getPlatform().getStackVersionOrJavaVersion());
        } else if (settings.getOS() == OperatingSystem.WINDOWS) {
            settings.setWebContainer(config.getPlatform().getStackOrWebContainer());
            settings.setJdkVersion(JavaVersion.fromString(config.getPlatform().getStackVersionOrJavaVersion()));
        }
        // creating if id is empty
        settings.setCreatingAppServicePlan(config.getServicePlan() instanceof Draft || StringUtils.isEmpty(config.getServicePlan().id()));
        if (settings.isCreatingAppServicePlan()) {
            settings.setAppServicePlanName(config.getServicePlan().name());
        } else {
            settings.setAppServicePlanId(config.getServicePlan().id());
        }
        settings.setPricing(config.getServicePlan().pricingTier().toString());
        final MonitorConfig monitorConfig = config.getMonitorConfig();
        if (monitorConfig != null) {
            settings.setEnableApplicationLog(monitorConfig.isEnableApplicationLog());
            settings.setApplicationLogLevel(monitorConfig.getApplicationLogLevel());
            settings.setEnableWebServerLogging(monitorConfig.isEnableWebServerLogging());
            settings.setWebServerLogQuota(monitorConfig.getWebServerLogQuota());
            settings.setWebServerRetentionPeriod(monitorConfig.getWebServerRetentionPeriod());
            settings.setEnableDetailedErrorMessage(monitorConfig.isEnableDetailedErrorMessage());
            settings.setEnableFailedRequestTracing(monitorConfig.isEnableFailedRequestTracing());
        }
        return settings;
    }
}
