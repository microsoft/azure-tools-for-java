package com.microsoft.azuretools.webapp.utils;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.util.Map;

public class TelemetryUtil {

    public static void sendTelemetryOpStart(String operationName, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .startTransaction(TelemetryConstants.PRODUCTION_NAME_WEBAPP, operationName, properties);
    }

    public static void sendTelemetryOpEnd(String operationName, Map<String, String> properties,
        long time) {
        TelemetryManager.getInstance().getProducer()
            .endTransaction(TelemetryConstants.PRODUCTION_NAME_WEBAPP, operationName, properties, time);
    }

    public static void sendTelemetryInfo(String operationName, Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .sendInfo(TelemetryConstants.PRODUCTION_NAME_WEBAPP, operationName, properties);
    }

    public static void sendTelemetryOpError(String operationName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        TelemetryManager.getInstance().getProducer()
            .sendError(TelemetryConstants.PRODUCTION_NAME_WEBAPP, operationName, errorType, errMsg, properties);
    }
}
