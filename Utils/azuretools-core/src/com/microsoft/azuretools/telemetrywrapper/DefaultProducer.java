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

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultProducer implements Producer {
    private static ThreadLocal<String> operIDS = new ThreadLocal<>();
    private static ThreadLocal<Map<String, String>> errorInfo = new ThreadLocal<>();
    private static final String OPERATION_NAME = "operationName";
    private static final String OPERATION_ID = "operationId";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MSG = "message";
    private static final String ERROR_TYPE = "errorType";
    private static final String DURATION = "duration";
    private TelemetryClient client;

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
            Map<String, String> mergedProperty = mergeProperties(addOperNameAndId(properties, operName));
            if (errorInfo.get() != null) {
                mergedProperty.putAll(errorInfo.get());
            }
            sendTelemetry(EventType.opEnd, eventName, mergedProperty, metrics);
        } catch (Exception ignore) {
        } finally {
            errorInfo.remove();
            operIDS.remove();
        }
    }

    @Override
    public void sendError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        try {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(ERROR_CODE, "1");
            errorMap.put(ERROR_MSG, errMsg);
            errorMap.put(ERROR_TYPE, errorType.name());
            // we need to save errorinfo, and then write the error info when we end the transaction, by this way we
            // can quickly get the operation result from opend
            errorInfo.remove();
            errorInfo.set(errorMap);

            Map<String, String> newProperties = addOperNameAndId(properties, operName);
            newProperties.putAll(errorMap);
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

    @Override
    public void setTelemetryClient(TelemetryClient client) {
        this.client = client;
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
        if (client != null) {
            client.trackEvent(getFullEventName(eventName, eventType), properties, metrics);
            client.flush();
        }
    }

    private String getFullEventName(String eventName, EventType eventType) {
        return TelemetryManager.getInstance().getEventNamePrefix() + eventName + "/" + eventType.name();
    }
}
