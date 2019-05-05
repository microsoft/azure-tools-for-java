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

import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.DURATION;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.ERROR_CLASSNAME;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.ERROR_CODE;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.ERROR_MSG;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.ERROR_TYPE;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.OPERATION_ID;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.OPERATION_NAME;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.mergeProperties;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.sendTelemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            if (properties == null) {
                properties = new HashMap<>();
            }
            if (metrics == null) {
                metrics = new HashMap<>();
            }
            properties.put(OPERATION_ID, operationId);
            properties.put(OPERATION_NAME, operationName);

            if (eventType == EventType.step) {
                metrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            }
            sendTelemetry(eventType, serviceName, mergeProperties(properties), metrics);
        } catch (Exception ignore) {
        }
    }

    public synchronized void logError(ErrorType errorType, Exception e, Map<String, String> properties,
        Map<String, Double> metrics) {
        try {
            if (isComplete) {
                return;
            }
            if (properties == null) {
                properties = new HashMap<>();
            }
            if (metrics == null) {
                metrics = new HashMap<>();
            }
            error = new Error();
            error.errorType = errorType == null ? ErrorType.systemError : errorType;
            error.errMsg = e == null ? "" : e.getMessage();
            error.className = e == null ? "" : e.getClass().getName();

            properties.put(ERROR_CODE, "1");
            properties.put(ERROR_MSG, error.errMsg);
            properties.put(ERROR_TYPE, error.errorType.name());
            properties.put(ERROR_CLASSNAME, error.className);
            properties.put(OPERATION_ID, operationId);
            properties.put(OPERATION_NAME, operationName);

            metrics.put(DURATION, Double.valueOf(System.currentTimeMillis() - timeStart));
            sendTelemetry(EventType.error, serviceName, mergeProperties(properties), metrics);
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
    }

}
