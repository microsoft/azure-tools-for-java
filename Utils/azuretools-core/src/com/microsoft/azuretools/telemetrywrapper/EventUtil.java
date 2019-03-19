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

public class EventUtil {

    public static void logEvent(EventType eventType, String eventName, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(CommonUtil.OPERATION_NAME, operName);
        properties.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
        sendTelemetry(eventType, eventName, mergeProperties(properties), metrics);
    }

    public static void logError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties, Map<String, Double> metrics) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(CommonUtil.OPERATION_NAME, operName);
        properties.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
        properties.put(CommonUtil.ERROR_CODE, "1");
        properties.put(CommonUtil.ERROR_MSG, errMsg);
        properties.put(CommonUtil.ERROR_TYPE, errorType.name());
        sendTelemetry(EventType.error, eventName, mergeProperties(properties), metrics);
    }
}
