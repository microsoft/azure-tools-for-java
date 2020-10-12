package com.microsoft.azure.appservice.webapp;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azuretools.telemetrywrapper.*;

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

    private static WebAppSettingModel convertConfig2Settings(final WebAppConfig config) {
        return null;
    }
}
