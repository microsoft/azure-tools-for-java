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

package com.microsoft.azuretools.telemetrywrapper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.*;

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
        properties.put(CommonUtil.OPERATION_NAME, operationName);
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

    public synchronized void logError(ErrorType errorType, Throwable e, Map<String, String> properties,
        Map<String, Double> metrics) {
        try {
            if (isComplete) {
                return;
            }
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            Map<String, Double> mutableMetrics = metrics == null ? new HashMap<>() : new HashMap<>(metrics);

            error = new Error();
            error.errorType = errorType == null ? ErrorType.systemError : errorType;
            error.errMsg = e == null ? "" : e.getMessage();
            error.className = e == null ? "" : e.getClass().getName();
            error.stackTrace = ExceptionUtils.getStackTrace(e);

            mutableProps.put(ERROR_CODE, "1");
            mutableProps.put(ERROR_MSG, error.errMsg);
            mutableProps.put(ERROR_TYPE, error.errorType.name());
            mutableProps.put(ERROR_CLASSNAME, error.className);
            mutableProps.put(ERROR_STACKTRACE, error.stackTrace);
            mutableProps.put(OPERATION_ID, operationId);
            mutableProps.put(OPERATION_NAME, operationName);

            mutableMetrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), mutableMetrics);
        } catch (Exception ignore) {
        }
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public synchronized void logErrorClassNameOnly(ErrorType errorType, Throwable e, Map<String, String> properties,
                                      Map<String, Double> metrics) {
        try {
            if (isComplete) {
                return;
            }
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            Map<String, Double> mutableMetrics = metrics == null ? new HashMap<>() : new HashMap<>(metrics);

            error = new Error();
            error.errorType = errorType == null ? ErrorType.systemError : errorType;
            error.className = e == null ? "" : e.getClass().getName();

            mutableProps.put(ERROR_CODE, "1");
            mutableProps.put(ERROR_TYPE, error.errorType.name());
            mutableProps.put(ERROR_CLASSNAME, error.className);
            mutableProps.put(OPERATION_ID, operationId);
            mutableProps.put(OPERATION_NAME, operationName);

            mutableMetrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), mutableMetrics);
        } catch (Exception ignore) {
        }
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
    public void close() {
        complete();
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
