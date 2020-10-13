package com.microsoft.azure.toolkit.appservice.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.telemetrywrapper.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_WEBAPP;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

public class WebAppService {
    public static WebAppService instance = new WebAppService();

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
        settings.setCreatingResGrp(StringUtils.isEmpty(config.getResourceGroup().id()));
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
        settings.setCreatingAppServicePlan(StringUtils.isEmpty(config.getServicePlan().id()));
        settings.setAppServicePlanId(config.getServicePlan().id());
        settings.setPricing(config.getServicePlan().pricingTier().toString());
        return settings;
    }
}
