/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.ERROR_CLASSNAME;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.ERROR_CODE;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.ERROR_MSG;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.ERROR_STACKTRACE;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.ERROR_TYPE;
import static com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter.OPERATION_NAME;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.DURATION;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.OPERATION_ID;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.mergeProperties;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.sendTelemetry;
import static com.microsoft.azuretools.telemetrywrapper.EventUtil.isAbleToCollectErrorStacks;

@Getter
public class DefaultOperation implements Operation {

    private long timeStart;
    private String operationId;
    private String serviceName;
    private String operationName;
    private Error error;
    private Map<String, String> properties;
    private volatile boolean isComplete = false;

    public DefaultOperation(String serviceName, String operationName) {
        this.serviceName = serviceName == null ? "" : serviceName;
        this.operationName = operationName == null ? "" : operationName;
        this.operationId = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
        properties.put(CommonUtil.OPERATION_ID, operationId);
        properties.put(OPERATION_NAME, operationName);
    }

    public void logEvent(EventType eventType, Map<String, String> properties, Map<String, Double> metrics) {
        try {
            if (isComplete) {
                return;
            }
            if (eventType == EventType.opStart || eventType == EventType.opEnd) {
                return;
            }
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(OPERATION_ID, operationId);
            mutableProps.put(OPERATION_NAME, operationName);

            Map<String, Double> mutableMetrics = metrics == null ? new HashMap<>() : new HashMap<>(metrics);
            if (eventType == EventType.step) {
                mutableMetrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            }
            sendTelemetry(eventType, serviceName, mergeProperties(mutableProps), mutableMetrics);
        } catch (Exception ignore) {
        }
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public synchronized void logErrorClassNameOnly(ErrorType errorType, Throwable e, Map<String, String> properties,
                                                   Map<String, Double> metrics) {
        logError(errorType, e, properties, metrics, false);
    }

    public synchronized void logError(ErrorType errorType, Throwable e, Map<String, String> properties,
                                      Map<String, Double> metrics) {
        logError(errorType, e, properties, metrics, true);
    }

    @Override
    public synchronized void start() {
        try {
            if (isComplete) {
                return;
            }
            timeStart = System.currentTimeMillis();
            sendTelemetry(EventType.opStart, serviceName, mergeProperties(properties), null);
        } catch (Exception ignore) {
        }
    }

    @Override
    public synchronized void complete() {
        if (isComplete) {
            return;
        }
        try {
            Map<String, Double> metrics = new HashMap<>();
            metrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            Map<String, String> mergedProperty = mergeProperties(properties);
            if (error != null) {
                mergedProperty.put(ERROR_CODE, "1");
                mergedProperty.put(ERROR_MSG, error.errMsg);
                mergedProperty.put(ERROR_TYPE, error.errorType.name());
                mergedProperty.put(ERROR_CLASSNAME, error.className);
                mergedProperty.put(ERROR_STACKTRACE, error.stackTrace);

            }
            sendTelemetry(EventType.opEnd, serviceName, mergedProperty, metrics);
        } catch (Exception ignore) {
        } finally {
            clear();
        }
    }

    @Override
    public void trackProperty(String key, @Nullable String value) {
        properties.put(key, value);
    }

    @Override
    public void trackProperties(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    @Override
    public void close() {
        complete();
    }

    private synchronized void logError(ErrorType errorType, Throwable e, Map<String, String> properties,
                                       Map<String, Double> metrics, boolean logErrorTraces) {
        try {
            if (isComplete) {
                return;
            }
            error = new Error();
            error.errorType = errorType == null ? ErrorType.systemError : errorType;
            error.errMsg = e == null ? "" : e.getMessage();
            error.className = e == null ? "" : e.getClass().getName();
            error.stackTrace = ExceptionUtils.getStackTrace(e);

            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(ERROR_CODE, "1");
            mutableProps.put(ERROR_TYPE, error.errorType.name());
            mutableProps.put(ERROR_CLASSNAME, error.className);
            if (logErrorTraces && isAbleToCollectErrorStacks()) {
                mutableProps.put(ERROR_MSG, error.errMsg);
                mutableProps.put(ERROR_STACKTRACE, error.stackTrace);
            }
            mutableProps.put(OPERATION_ID, operationId);
            mutableProps.put(OPERATION_NAME, operationName);

            Map<String, Double> mutableMetrics = metrics == null ? new HashMap<>() : new HashMap<>(metrics);
            mutableMetrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), mutableMetrics);
        } catch (Exception ignore) {
        }
    }

    private void clear() {
        isComplete = true;
    }

    private static class Error {
        ErrorType errorType;
        String errMsg;
        String className;
        String stackTrace;
    }

}
