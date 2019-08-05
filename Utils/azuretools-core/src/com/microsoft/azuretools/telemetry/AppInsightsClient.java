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
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AppInsightsClient {
    static AppInsightsConfiguration configuration;

    public enum EventType {
        Action,
        Dialog,
        Error,
        WizardStep,
        Telemetry,
        DockerContainer,
        DockerHost,
        WebApp,
        Plugin,
        Subscription,
        Azure
    }

    public static String getInstallationId(){
        return configuration == null ? null : configuration.installationId();
    }

    public static void setAppInsightsConfiguration(AppInsightsConfiguration appInsightsConfiguration) {
        if (appInsightsConfiguration == null)
            throw new NullPointerException("AppInsights configuration cannot be null.");
        configuration = appInsightsConfiguration;
        initTelemetryManager();
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
        create(eventName, version, myProperties, null, force);
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
        if (!StringUtils.isNullOrEmpty(pluginVersion)) {
            properties.put("Plugin Version", pluginVersion);
        }

        String instID = configuration.installationId();
        if (!StringUtils.isNullOrEmpty(instID)) {
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

    private static void initTelemetryManager() {
        try {
            TelemetryManager.getInstance().setCommonProperties(buildProperties("", new HashMap<>()));
            TelemetryManager.getInstance().setTelemetryClient(TelemetryClientSingleton.getTelemetry());
            TelemetryManager.getInstance().setEventNamePrefix(configuration.eventName());
        } catch (Exception ignore) {
        }
    }

}
