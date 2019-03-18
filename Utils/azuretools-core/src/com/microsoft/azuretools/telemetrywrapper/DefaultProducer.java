package com.microsoft.azuretools.telemetrywrapper;

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultProducer implements Producer {

    private static ThreadLocal<String> operIDS = new ThreadLocal<>();
    private static final String OPERATION_NAME = "operationName";
    private static final String OPERATION_ID = "operationId";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MSG = "message";
    private static final String ERROR_TYPE = "errorType";
    private static final String DURATION = "duration";

    @Override
    public void startTransaction(String eventName, String operName, Map<String, String> properties) {
        try {
            operIDS.remove();
            operIDS.set(UUID.randomUUID().toString());
            sendTelemetry(EventType.opStart, eventName, mergeProperties(addOperNameAndId(properties, operName)), null);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void endTransaction(String eventName, String operName, Map<String, String> properties, long time) {
        try {
            Map<String, Double> metrics = new HashMap<>();
            metrics.put(DURATION, Double.valueOf(time));
            sendTelemetry(EventType.opEnd, eventName, mergeProperties(addOperNameAndId(properties, operName)), metrics);
            operIDS.remove();
        } catch (Exception ignore) {
        }
    }

    @Override
    public void sendError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        try {
            Map<String, String> newProperties = addOperNameAndId(properties, operName);
            newProperties.put(ERROR_CODE, "1");
            newProperties.put(ERROR_MSG, errMsg);
            newProperties.put(ERROR_TYPE, errorType.name());
            sendTelemetry(EventType.error, eventName, mergeProperties(newProperties), null);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void sendInfo(String eventName, String operName, Map<String, String> properties) {
        try {
            sendTelemetry(EventType.info, eventName, mergeProperties(addOperNameAndId(properties, operName)), null);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void sendWarn(String eventName, String operName, Map<String, String> properties) {
        try {
            sendTelemetry(EventType.warn, eventName, mergeProperties(addOperNameAndId(properties, operName)), null);
        } catch (Exception ignore) {
        }
    }

    private Map<String, String> addOperNameAndId(Map<String, String> properties, String operName) {
        Map<String, String> result = new HashMap<>();
        if (properties != null) {
            result.putAll(properties);
        }
        result.put(OPERATION_NAME, operName);
        String operId = operIDS.get();
        result.put(OPERATION_ID, operId == null ? UUID.randomUUID().toString() : operId);
        return result;
    }

    private Map<String, String> mergeProperties(Map<String, String> properties) {
        Map<String, String> commonProperties = TelemetryManager.getInstance().getCommonProperties();
        Map<String, String> merged = new HashMap<>();
        if (properties == null) {
            properties = new HashMap<>();
        }
        merged.putAll(commonProperties);
        merged.putAll(properties);
        return merged;
    }

    private synchronized void sendTelemetry(EventType eventType, String eventName, Map<String, String> properties,
        Map<String, Double> metrics) {
        TelemetryClient telemetry = TelemetryManager.getInstance().getTelemetryClient();
        if (telemetry != null) {
            telemetry.trackEvent(getFullEventName(eventName, eventType), properties, metrics);
            telemetry.flush();
        }
    }

    private String getFullEventName(String eventName, EventType eventType) {
        return TelemetryManager.getInstance().getEventNamePrefix() + eventName + "/" + eventType.name();
    }
}
