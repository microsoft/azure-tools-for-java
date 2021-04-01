/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azuretools.adauth.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private static List<MutableTriple<EventType, Map, Map>> cachedEvents = new ArrayList<>();

    public static Map<String, String> mergeProperties(Map<String, String> properties) {
        Map<String, String> commonProperties = TelemetryManager.getInstance().getCommonProperties();
        Map<String, String> merged = new HashMap<>(commonProperties);
        if (properties != null) {
            merged.putAll(properties);
        }
        return merged;
    }

    public static synchronized void sendTelemetry(EventType eventType, String serviceName, Map<String, String> properties,
        Map<String, Double> metrics) {
        Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
        // Tag UTC time as timestamp
        mutableProps.put(TIMESTAMP, Instant.now().toString());
        if (!StringUtils.isNullOrEmpty(serviceName)) {
            mutableProps.put(SERVICE_NAME, serviceName);
        }
        if (client != null) {
            final String eventName = getFullEventName(eventType);
            client.trackEvent(eventName, mutableProps, metrics);
            client.flush();
        } else {
            cacheEvents(eventType, mutableProps, metrics);
        }
    }

    public static void clearCachedEvents() {
        if (client != null) {
            cachedEvents.forEach(triple -> client.trackEvent(getFullEventName(triple.left), triple.middle, triple.right));
            client.flush();
            cachedEvents.clear();
        }
    }

    private static void cacheEvents(EventType eventType, Map<String, String> mutableProps, Map<String, Double> metrics) {
        cachedEvents.add(MutableTriple.of(eventType, mutableProps, metrics));
    }

    private static String getFullEventName(EventType eventType) {
        return TelemetryManager.getInstance().getEventNamePrefix() + "/" + eventType.name();
    }

}
