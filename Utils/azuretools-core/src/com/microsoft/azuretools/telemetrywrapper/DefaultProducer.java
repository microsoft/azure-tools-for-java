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

import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.mergeProperties;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.sendTelemetry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultProducer implements Producer {
    private static ThreadLocal<String> operIDTL = new ThreadLocal<>();
    private static ThreadLocal<Long> startTimeTL = new ThreadLocal<>();
    private static ThreadLocal<Map<String, String>> errorInfoTL = new ThreadLocal<>();
    private static ThreadLocal<EventAndOper> eventAndOperTL = new ThreadLocal<>();

    @Override
    public void startTransaction(String eventName, String operName) {
        try {
            if (operIDTL.get() != null) {
                clearThreadLocal();
            }
            operIDTL.set(UUID.randomUUID().toString());
            startTimeTL.set(System.currentTimeMillis());
            eventAndOperTL.set(new EventAndOper(eventName, operName));
            sendTelemetry(EventType.opStart, eventName, mergeProperties(addOperNameAndId(null, operName)), null);
        } catch (Exception ignore) {
        }
    }

    @Override
    public void endTransaction() {
        try {
            EventAndOper eventAndOper = eventAndOperTL.get();
            if (eventAndOper == null) {
                return;
            }
            HashMap<String, Double> metrics = new HashMap<>();
            Long time = startTimeTL.get();
            long timeDuration = time == null ? 0 : System.currentTimeMillis() - time;
            metrics.put(CommonUtil.DURATION, Double.valueOf(timeDuration));
            Map<String, String> mergedProperty = mergeProperties(addOperNameAndId(null, eventAndOper.operName));

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
            errorMap.put(CommonUtil.ERROR_CODE, "1");
            errorMap.put(CommonUtil.ERROR_MSG, errMsg);
            errorMap.put(CommonUtil.ERROR_TYPE, errorType.name());
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

    private Map<String, String> addOperNameAndId(Map<String, String> properties, String operName) {
        Map<String, String> result = new HashMap<>();
        if (properties != null) {
            result.putAll(properties);
        }
        result.put(CommonUtil.OPERATION_NAME, operName);
        String operId = operIDTL.get();
        result.put(CommonUtil.OPERATION_ID, operId == null ? UUID.randomUUID().toString() : operId);
        return result;
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