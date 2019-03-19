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
    private static ThreadLocal<String> operIDTL = new ThreadLocal<>();
    private static ThreadLocal<Long> startTimeTL = new ThreadLocal<>();
    private static ThreadLocal<Map<String, String>> errorInfoTL = new ThreadLocal<>();
    private static ThreadLocal<EventAndOper> eventAndOperTL = new ThreadLocal<>();
    private static final String OPERATION_NAME = "operationName";
    private static final String OPERATION_ID = "operationId";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MSG = "message";
    private static final String ERROR_TYPE = "errorType";
    private static final String DURATION = "duration";
    private TelemetryClient client;

    @Override
    public void startTransaction(String eventName, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        try {
            if (operIDTL.get() != null) {
                clearThreadLocal();
            }
            operIDTL.set(UUID.randomUUID().toString());
            startTimeTL.set(System.currentTimeMillis());
            eventAndOperTL.set(new EventAndOper(eventName, operName));
            sendTelemetry(EventType.opStart, eventName, mergeProperties(addOperNameAndId(properties, operName)),
                metrics);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void endTransaction(Map<String, String> properties, Map<String, Double> metrics) {
        try {
            EventAndOper eventAndOper = eventAndOperTL.get();
            if (eventAndOper == null) {
                return;
            }
            if (metrics == null) {
                metrics = new HashMap<>();
            }
            Long time = startTimeTL.get();
            long timeDuration = time == null ? 0 : System.currentTimeMillis() - time;
            metrics.put(DURATION, Double.valueOf(timeDuration));
            Map<String, String> mergedProperty = mergeProperties(addOperNameAndId(properties, eventAndOper.operName));

            Map<String, String> errorInfo = errorInfoTL.get();
            if (errorInfo != null) {
                mergedProperty.putAll(errorInfo);
            }
            sendTelemetry(EventType.opEnd, eventAndOper.eventName, mergedProperty, metrics);
        } catch (Exception ignore) {
        } finally {
            clearThreadLocal();
        }
    }

    @Override
    public void sendError(ErrorType errorType, String errMsg, Map<String, String> properties,
        Map<String, Double> metrics) {
        try {
            EventAndOper eventAndOper = eventAndOperTL.get();
            if (eventAndOper == null) {
                return;
            }
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put(ERROR_CODE, "1");
            errorMap.put(ERROR_MSG, errMsg);
            errorMap.put(ERROR_TYPE, errorType.name());
            // we need to save errorinfo, and then write the error info when we end the transaction, by this way we
            // can quickly get the operation result from opend
            errorInfoTL.set(errorMap);
            Map<String, String> newProperties = addOperNameAndId(properties, eventAndOper.operName);
            newProperties.putAll(errorMap);
            sendTelemetry(EventType.error, eventAndOper.eventName, mergeProperties(newProperties), metrics);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void sendInfo(Map<String, String> properties, Map<String, Double> metrics) {
        try {
            EventAndOper eventAndOper = eventAndOperTL.get();
            if (eventAndOper == null) {
                return;
            }
            sendTelemetry(EventType.info, eventAndOper.eventName,
                mergeProperties(addOperNameAndId(properties, eventAndOper.operName)), metrics);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void sendWarn(Map<String, String> properties, Map<String, Double> metrics) {
        try {
            EventAndOper eventAndOper = eventAndOperTL.get();
            if (eventAndOper == null) {
                return;
            }
            sendTelemetry(EventType.warn, eventAndOper.eventName,
                mergeProperties(addOperNameAndId(properties, eventAndOper.operName)), metrics);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void logEvent(EventType eventType, String eventName, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(OPERATION_NAME, operName);
        properties.put(OPERATION_ID, UUID.randomUUID().toString());
        sendTelemetry(eventType, eventName, mergeProperties(properties), metrics);
    }

    @Override
    public void logError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties, Map<String, Double> metrics) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(OPERATION_NAME, operName);
        properties.put(OPERATION_ID, UUID.randomUUID().toString());
        properties.put(ERROR_CODE, "1");
        properties.put(ERROR_MSG, errMsg);
        properties.put(ERROR_TYPE, errorType.name());
        sendTelemetry(EventType.error, eventName, mergeProperties(properties), metrics);
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
        String operId = operIDTL.get();
        result.put(OPERATION_ID, operId == null ? UUID.randomUUID().toString() : operId);
        return result;
    }

    private Map<String, String> mergeProperties(Map<String, String> properties) {
        Map<String, String> commonProperties = TelemetryManager.getInstance().getCommonProperties();
        Map<String, String> merged = new HashMap<>(commonProperties);
        if (properties != null) {
            merged.putAll(properties);
        }
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

    private void clearThreadLocal() {
        errorInfoTL.remove();
        operIDTL.remove();
        startTimeTL.remove();
        eventAndOperTL.remove();
    }

    private static class EventAndOper {
        String eventName;
        String operName;

        public EventAndOper(String eventName, String operName) {
            this.eventName = eventName;
            this.operName = operName;
        }
    }
}