/*
 * Copyright (c) Microsoft Corporation
 *   <p/>
 *  All rights reserved.
 *   <p/>
 *  MIT License
 *   <p/>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 *  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  <p/>
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  the Software.
 *   <p/>
 *  THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 *  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class AppInsightsClient {
    private static final String EVENT_SPLIT = "/";
    private static final String OPERATION_ID = "operationid";
    private static final String OPERATION_NAME = "operationname";
    private static final String ERROR_CODE = "errorCode";
    private static final String ERROR_MSG = "message";
    private static final String ERROR_TYPE = "errorType";


    static AppInsightsConfiguration configuration;

    public enum EventType {
        Action,
        Dialog,
        WizardStep,
        Telemetry,
        DockerContainer,
        DockerHost,
        WebApp,
        Plugin,
        Subscription,
        Azure
    }

    public enum EventName {
        opStart,
        opEnd,
        error
    }

    public enum ErrorType {
        userError,
        SystemError
    }

    public static void setAppInsightsConfiguration(AppInsightsConfiguration appInsightsConfiguration) {
        if (appInsightsConfiguration == null)
            throw new NullPointerException("AppInsights configuration cannot be null.");
        configuration = appInsightsConfiguration;
    }

    @Nullable
    public static String getConfigurationSessionId() {
        return configuration == null ? null : configuration.sessionId();
    }

    public static void createByType(final EventType eventType, final String objectName, final String action) {
        if (!isAppInsightsClientAvailable())
            return;

        createByType(eventType, objectName, action, null);
    }

    public static void createByType(final EventType eventType, final String objectName, final String action, final Map<String, String> properties) {
        if (!isAppInsightsClientAvailable())
            return;

        createByType(eventType, objectName, action, properties, false);
    }

    public static void createByType(final EventType eventType, final String objectName, final String action, final Map<String, String> properties, final boolean force) {
        if (!isAppInsightsClientAvailable())
            return;

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(configuration.eventNamePrefix()).append(eventType.name());
        if (!StringUtils.isNullOrEmpty(objectName))
            stringBuilder.append(".").append(objectName.replaceAll("[\\s+.]", ""));
        if (!StringUtils.isNullOrEmpty(action)) stringBuilder.append(".").append(action.replaceAll("[\\s+.]", ""));
        create(stringBuilder.toString(), null, properties, force);
    }

    public static void create(String eventName, String version) {
        if (!isAppInsightsClientAvailable())
            return;

        create(eventName, version, null);
    }

    public static void create(String eventName, String version, @Nullable Map<String, String> myProperties) {
        if (!isAppInsightsClientAvailable())
            return;

        create(eventName, version, myProperties, false);
    }

    public static void create(String eventName, String version, @Nullable Map<String, String> myProperties, boolean force) {
        if (!isAppInsightsClientAvailable())
            return;

        if (configuration.validated()) {
            String prefValue = configuration.preferenceVal();
            if (prefValue == null || prefValue.isEmpty() || prefValue.equalsIgnoreCase("true") || force) {
                TelemetryClient telemetry = TelemetryClientSingleton.getTelemetry();
                Map<String, String> properties = buildProperties(version, myProperties);
                synchronized (TelemetryClientSingleton.class) {
                    telemetry.trackEvent(eventName, properties, null);
                    telemetry.flush();
                }
            }
        }
    }

    private static void create(String eventName, String version, @Nullable Map<String, String> myProperties,
        Map<String, Double> metrics, boolean force) {
        if (isAppInsightsClientAvailable() && configuration.validated()) {
            String prefValue = configuration.preferenceVal();
            if (prefValue == null || prefValue.isEmpty() || prefValue.equalsIgnoreCase("true") || force) {
                TelemetryClient telemetry = TelemetryClientSingleton.getTelemetry();
                Map<String, String> properties = buildProperties(version, myProperties);
                synchronized (TelemetryClientSingleton.class) {
                    telemetry.trackEvent(eventName, properties, metrics);
                    telemetry.flush();
                }
            }
        }
    }

    private static Map<String, String> buildProperties(String version, Map<String, String> myProperties) {
        Map<String, String> properties = myProperties == null ? new HashMap<>() : new HashMap<>(myProperties);
        properties.put("SessionId", configuration.sessionId());
        properties.put("IDE", configuration.ide());

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null as key or value..
        for (Iterator<Map.Entry<String, String>> iter = properties.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<String, String> entry = iter.next();
            if (StringUtils.isNullOrEmpty(entry.getKey()) || StringUtils.isNullOrEmpty(entry.getValue())) {
                iter.remove();
            }
        }
        if (version != null && !version.isEmpty()) {
            properties.put("Library Version", version);
        }
        String pluginVersion = configuration.pluginVersion();
        if (pluginVersion != null && !pluginVersion.isEmpty()) {
            properties.put("Plugin Version", pluginVersion);
        }

        String instID = configuration.installationId();
        if (instID != null && !instID.isEmpty()) {
            properties.put("Installation ID", instID);
        }
        return properties;
    }

    public static void createFTPEvent(String eventName, String uri, String appName, String subId) {
        if (!isAppInsightsClientAvailable())
            return;

        TelemetryClient telemetry = TelemetryClientSingleton.getTelemetry();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("SessionId", configuration.sessionId());
        if (uri != null && !uri.isEmpty()) {
            properties.put("WebApp URI", uri);
        }
        if (appName != null && !appName.isEmpty()) {
            properties.put("Java app name", appName);
        }
        if (subId != null && !subId.isEmpty()) {
            properties.put("Subscription ID", subId);
        }
        if (configuration.validated()) {
            String pluginVersion = configuration.pluginVersion();
            if (pluginVersion != null && !pluginVersion.isEmpty()) {
                properties.put("Plugin Version", pluginVersion);
            }

            String instID = configuration.installationId();
            if (instID != null && !instID.isEmpty()) {
                properties.put("Installation ID", instID);
            }
        }
        synchronized(TelemetryClientSingleton.class){
            telemetry.trackEvent(eventName, properties, null);
            telemetry.flush();
        }
    }

    private static boolean isAppInsightsClientAvailable() {
        return configuration != null;
    }


    public static void sendOpEnd(EventType eventType, String operName, Map<String, String> properties) {
        sendOpEnd2(eventType, operName, properties, null);
    }

    public static void sendOpEnd(EventType eventType, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        sendOpEnd2(eventType, operName, properties, metrics);
    }

    private static void sendOpEnd2(EventType eventType, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        properties.put(OPERATION_NAME, operName);
        properties.put(OPERATION_ID, UUID.randomUUID().toString());

        String eventName = getEventName(eventType, EventName.opEnd);
        create(eventName, null, properties, metrics, false);
    }

    public static void sendOpStart(EventType eventType, String operName, Map<String, String> properties) {
        properties.put(OPERATION_NAME, operName);
        properties.put(OPERATION_ID, UUID.randomUUID().toString());

        String eventName = getEventName(eventType, EventName.opStart);
        create(eventName, null, properties, false);
    }

    public static void sendError(EventType eventType, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        sendError2(eventType, operName, errorType, errMsg, properties, null);
    }

    public static void sendError(EventType eventType, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties, Map<String, Double> metrics) {
        sendError2(eventType, operName, errorType, errMsg, properties, metrics);
    }

    private static void sendError2(EventType eventType, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties, Map<String, Double> metrics) {

        properties.put(OPERATION_NAME, operName);
        properties.put(OPERATION_ID, UUID.randomUUID().toString());
        properties.put(ERROR_CODE, "1");
        properties.put(ERROR_MSG, errMsg);
        properties.put(ERROR_TYPE, errorType.name());

        String eventName = getEventName(eventType, EventName.error);
        create(eventName, null, properties, metrics, false);
    }

    private static String getEventName(EventType eventType, EventName eventName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(configuration.eventNamePrefix()).append(eventType.name()).append(EVENT_SPLIT)
            .append(eventName.name());
        return stringBuilder.toString();
    }

}
