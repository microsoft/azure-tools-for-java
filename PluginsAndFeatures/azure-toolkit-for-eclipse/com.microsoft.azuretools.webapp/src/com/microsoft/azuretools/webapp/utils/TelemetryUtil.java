package com.microsoft.azuretools.webapp.utils;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.util.Map;

public class TelemetryUtil {

    public static void sendTelemetryOpStart(String operationName, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .startTransaction(TelemetryConstants.WEBAPP, operationName, properties, null);
    }

    public static void sendTelemetryOpEnd(Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer().endTransaction(properties, null);
    }

    public static void sendTelemetryInfo(Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer().sendInfo(properties, null);
    }

    public static void sendTelemetryOpError(ErrorType errorType, String errMsg, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer().sendError(errorType, errMsg, properties, null);
    }

    public static void logEvent(EventType eventType, String operName, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .logEvent(eventType, TelemetryConstants.WEBAPP, operName, properties, null);
    }

    public static void logError(String operName, ErrorType errorType, String errMsg, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .logError(TelemetryConstants.WEBAPP, operName, errorType, errMsg, properties, null);
    }
}
