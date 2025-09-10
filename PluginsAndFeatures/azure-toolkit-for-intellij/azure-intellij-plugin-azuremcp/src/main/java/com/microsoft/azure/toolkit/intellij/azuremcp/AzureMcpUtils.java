package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Map;

public final class AzureMcpUtils {

    private static final String GHCP_MCP_INITIALIZER = "GHCP_MCP_INITIALIZER";

    private AzureMcpUtils() {
    }

    public static void logTelemetryEvent(String eventName) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, GHCP_MCP_INITIALIZER,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, GHCP_MCP_INITIALIZER
        );
        AzureTaskManager.getInstance().runLater(() -> {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
        });
    }

    public static void logErrorTelemetryEvent(String eventName, Exception ex) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, GHCP_MCP_INITIALIZER,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, GHCP_MCP_INITIALIZER,
                AzureTelemeter.ERROR_STACKTRACE, ExceptionUtils.getStackTrace(ex)
        );
        AzureTaskManager.getInstance().runLater(() -> {
            AzureTelemeter.log(AzureTelemetry.Type.ERROR, properties);
        });
    }
}
