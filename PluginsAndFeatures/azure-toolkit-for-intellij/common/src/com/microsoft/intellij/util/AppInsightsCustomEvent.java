package com.microsoft.intellij.util;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.ui.messages.AzureBundle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;


public class AppInsightsCustomEvent {
    static String key = "9ee9694d-128e-4c2b-903f-dbe694548bf0";
    static String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));

    /**
     * @return resource filename in plugin's directory
     */
    private static String getTemplateFile(String fileName) {
        return String.format("%s%s%s", PluginUtil.getPluginRootDirectory(), File.separator, fileName);
    }

    public static void createTelemetryDenyEvent() {
        TelemetryClient telemetry = new TelemetryClient();
        telemetry.getContext().setInstrumentationKey(key);
        Map<String, String> properties = new HashMap<>();
        properties.put("Plugin Version", CommonConst.PLUGIN_VERISON);
        String instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
        if (instID != null && !instID.isEmpty()) {
            properties.put("Installation ID", instID);
        }
        telemetry.trackEvent(message("telemetryDenyAction"), properties, null);
        telemetry.flush();
    }

    public static void create(String eventName, String version, @Nullable Map<String, String> myProperties) {
        if (new File(dataFile).exists()) {
            String prefValue = DataOperations.getProperty(dataFile, AzureBundle.message("prefVal"));
            if (prefValue == null || prefValue.isEmpty() || prefValue.equalsIgnoreCase("true")) {
                TelemetryClient telemetry = new TelemetryClient();
                telemetry.getContext().setInstrumentationKey(key);
                Map<String, String> properties = myProperties == null ? new HashMap<String, String>() : new HashMap<String, String>(myProperties);
                if (version != null && !version.isEmpty()) {
                    properties.put("Library Version", version);
                }
                String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));
                if (pluginVersion != null && !pluginVersion.isEmpty()) {
                    properties.put("Plugin Version", pluginVersion);
                }

                String instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
                if (instID != null && !instID.isEmpty()) {
                    properties.put("Installation ID", instID);
                }

                telemetry.trackEvent(eventName, properties, null);
                telemetry.flush();
            }
        }
    }

    public static void create(String eventName, String version) {
        create(eventName, version, null);
    }

    public static void createFTPEvent(String eventName, String uri, String appName, String subId) {
        TelemetryClient telemetry = new TelemetryClient();
        telemetry.getContext().setInstrumentationKey(key);

        Map<String, String> properties = new HashMap<String, String>();

        if (uri != null && !uri.isEmpty()) {
            properties.put("WebApp URI", uri);
        }
        if (appName != null && !appName.isEmpty()) {
            properties.put("Java app name", appName);
        }
        if (subId != null && !subId.isEmpty()) {
            properties.put("Subscription ID", subId);
        }
        if (new File(dataFile).exists()) {
            String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));
            if (pluginVersion != null && !pluginVersion.isEmpty()) {
                properties.put("Plugin Version", pluginVersion);
            }

            String instID = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
            if (instID != null && !instID.isEmpty()) {
                properties.put("Installation ID", instID);
            }
        }
        telemetry.trackEvent(eventName, properties, null);
        telemetry.flush();
    }
}
