package com.microsoft.azuretools.webapp.utils;

import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsClient.ErrorType;
import com.microsoft.azuretools.telemetry.AppInsightsClient.EventType;
import java.util.Map;

public class TelemetryUtil {

    public static void sendTelemetryOpEnd(String operationName, Map<String, String> properties) {
        AppInsightsClient.sendOpEnd(EventType.WebApp, operationName, properties);
    }

    public static void sendTelemetryOpError(String operationName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        AppInsightsClient.sendError(EventType.WebApp, operationName, errorType, errMsg, properties);
    }


}
