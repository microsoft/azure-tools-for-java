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
import com.microsoft.azuretools.adauth.StringUtils;
import org.joda.time.Instant;

import java.util.HashMap;
import java.util.Map;

public class CommonUtil {

    public static final String OPERATION_NAME = "operationName";
    public static final String OPERATION_ID = "operationId";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MSG = "message";
    public static final String ERROR_TYPE = "errorType";
    public static final String ERROR_CLASSNAME = "errorClassName";
    public static final String ERROR_STACKTRACE = "errorStackTrace";
    public static final String DURATION = "duration";
    public static final String SERVICE_NAME = "serviceName";
    public static final String TIMESTAMP = "timestamp";
    public static TelemetryClient client;

    public static Map<String, String> mergeProperties(Map<String, String> properties) {
        Map<String, String> commonProperties = TelemetryManager.getInstance().getCommonProperties();
        Map<String, String> merged = new HashMap<>(commonProperties);
        if (properties != null) {
            merged.putAll(properties);
        }
        return merged;
    }

    public synchronized static void sendTelemetry(EventType eventType, String serviceName, Map<String, String> properties,
        Map<String, Double> metrics) {
        Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
        // Tag UTC time as timestamp
        mutableProps.put(TIMESTAMP, Instant.now().toString());
        if (client != null) {
            if (!StringUtils.isNullOrEmpty(serviceName)) {
                mutableProps.put(SERVICE_NAME, serviceName);
            }
            client.trackEvent(getFullEventName(eventType), mutableProps, metrics);
            client.flush();
        }
    }

    private static String getFullEventName(EventType eventType) {
        return TelemetryManager.getInstance().getEventNamePrefix() + "/" + eventType.name();
    }

}
