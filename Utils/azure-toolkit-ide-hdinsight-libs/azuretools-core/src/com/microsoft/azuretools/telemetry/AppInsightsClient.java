/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetry;

import com.microsoft.azure.toolkit.ide.common.experiment.ExperimentationClient;
import com.microsoft.azure.toolkit.ide.common.experiment.ExperimentationService;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        if (!StringUtils.isEmpty(objectName))
            stringBuilder.append(".").append(objectName.replaceAll("[\\s+.]", ""));
        if (!StringUtils.isEmpty(action)) stringBuilder.append(".").append(action.replaceAll("[\\s+.]", ""));
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
        properties.put("AssignmentContext", Optional.ofNullable(ExperimentationClient.getExperimentationService()).map(ExperimentationService::getAssignmentContext).orElse(StringUtils.EMPTY));

        // Telemetry client doesn't accept null value for ConcurrentHashMap doesn't accept null as key or value..
        properties.entrySet().removeIf(entry -> StringUtils.isEmpty(entry.getKey()) || StringUtils.isEmpty(entry.getValue()));
        if (version != null && !version.isEmpty()) {
            properties.put("Library Version", version);
        }
        String pluginVersion = configuration.pluginVersion();
        if (!StringUtils.isEmpty(pluginVersion)) {
            properties.put("Plugin Version", pluginVersion);
        }

        String instID = configuration.installationId();
        if (!StringUtils.isEmpty(instID)) {
            properties.put("Installation ID", instID);
        }
        return properties;
    }

    private static boolean isAppInsightsClientAvailable() {
        return configuration != null;
    }

    private static void initTelemetryManager() {
        try {
            final AzureTelemetryClient client = TelemetryClientSingleton.getTelemetry();
            final Map<String, String> clientDefaultProperties = Optional.ofNullable(client)
                    .map(AzureTelemetryClient::getDefaultProperties).orElse(Collections.emptyMap());
            final Map<String, String> toolkitDefaultProperties = buildProperties("", clientDefaultProperties);
            TelemetryClientSingleton.setConfiguration(configuration);
            final String eventNamePrefix = configuration.eventName();
            TelemetryManager.getInstance().setTelemetryClient(client);
            TelemetryManager.getInstance().setCommonProperties(toolkitDefaultProperties);
            TelemetryManager.getInstance().setEventNamePrefix(eventNamePrefix);
            TelemetryManager.getInstance().sendCachedTelemetries();
        } catch (Exception ignore) {
        }
    }
}
