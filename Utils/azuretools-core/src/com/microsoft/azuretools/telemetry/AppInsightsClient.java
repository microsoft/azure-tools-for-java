/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import java.util.HashMap;
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

    public static String getInstallationId() {
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

    public static void createByType(final EventType eventType, final String objectName, final String action, final Map<String, String> properties,
                                    final boolean force) {
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
                AzureTelemetryClient telemetry = TelemetryClientSingleton.getTelemetry();
                Map<String, String> properties = buildProperties(version, myProperties);
                synchronized (TelemetryClientSingleton.class) {
                    telemetry.trackEvent(eventName, properties, metrics);
                }
            }
        }
    }

    private static Map<String, String> buildProperties(String version, Map<String, String> myProperties) {
        Map<String, String> properties = myProperties == null ? new HashMap<>() : new HashMap<>(myProperties);
        properties.put("SessionId", configuration.sessionId());
        properties.put("IDE", configuration.ide());

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null as key or value..
        properties.entrySet().removeIf(entry -> StringUtils.isNullOrEmpty(entry.getKey()) || StringUtils.isNullOrEmpty(entry.getValue()));
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

    private static boolean isAppInsightsClientAvailable() {
        return configuration != null;
    }

    private static void initTelemetryManager() {
        try {
            final Map<String, String> properties = buildProperties("", new HashMap<>());
            TelemetryClientSingleton.setConfiguration(configuration);
            final AzureTelemetryClient client = TelemetryClientSingleton.getTelemetry();
            final String eventNamePrefix = configuration.eventName();
            TelemetryManager.getInstance().setTelemetryClient(client);
            TelemetryManager.getInstance().setCommonProperties(properties);
            TelemetryManager.getInstance().setEventNamePrefix(eventNamePrefix);
            TelemetryManager.getInstance().sendCachedTelemetries();
            AzureTelemeter.setClient(client);
            AzureTelemeter.setCommonProperties(properties);
            AzureTelemeter.setEventNamePrefix(eventNamePrefix);
        } catch (Exception ignore) {
        }
    }
}
