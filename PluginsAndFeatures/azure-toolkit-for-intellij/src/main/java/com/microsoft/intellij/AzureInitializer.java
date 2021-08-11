/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.PermanentInstallationID;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.util.ParserXMLUtility;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.rest.LogLevel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_INSTALL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_LOAD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PLUGIN_UPGRADE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.PROXY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureInitializer {
    public static void initialize() {
        ProxyUtils.initProxy();
        initializeAzureConfiguration();
    }

    private static void initializeAzureConfiguration() {
        final AzureConfiguration config = Azure.az().config();

        if (!AzureConfigurations.getInstance().loadToAzConfig()) {
            initializeConfigFromLegacySettings();
        }
        initializeDefaultValues();
        initializeTelemetry();

        CommonSettings.setUserAgent(config.getUserAgent());
        if (StringUtils.isNotBlank(config.getCloud())) {
            Azure.az(AzureCloud.class).setByName(config.getCloud());
        }
        AzureConfigurations.getInstance().saveAzConfig();
    }

    private static void initializeConfigFromLegacySettings() {
        final AzureConfiguration config = Azure.az().config();
        if (isDataFileValid()) {
            // read legacy settings from old data.xml
            try {
                final String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));
                final boolean allowTelemetry = Boolean.parseBoolean(DataOperations.getProperty(dataFile, AzureBundle.message("prefVal")));
                final String installationId = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
                final String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));

                // check non-empty for valid data.xml
                if (StringUtils.isNoneBlank(installationId, pluginVersion)) {
                    config.setTelemetryEnabled(allowTelemetry);
                    config.setVersion(pluginVersion);
                    if (InstallationIdUtils.isValidHashMac(installationId)) {
                        config.setMachineId(installationId);
                    }
                }
                FileUtils.deleteQuietly(new File(dataFile));
            } catch (Exception ex) {
                final Map<String, String> props = new HashMap<>();
                props.put("error", ex.getMessage());
                EventUtil.logEvent(EventType.error, SYSTEM, TelemetryConstants.PLUGIN_TRANSFER_SETTINGS, props, null);
            }
        }

        if (CommonSettings.getEnvironment() != null && StringUtils.isBlank(config.getCloud())) {
            // normalize cloud name
            config.setCloud(AzureEnvironmentUtils.azureEnvironmentToString(
                AzureEnvironmentUtils.stringToAzureEnvironment(CommonSettings.getEnvironment().getName())));
        }
    }

    private static void initializeDefaultValues() {
        final AzureConfiguration config = Azure.az().config();
        config.setLogLevel(LogLevel.NONE.name());
        if (StringUtils.isBlank(config.getMachineId())) {
            config.setMachineId(StringUtils.firstNonBlank(InstallationIdUtils.getHashMac(), InstallationIdUtils.hash(PermanentInstallationID.get())));
        }

        if (Objects.isNull(config.getTelemetryEnabled())) {
            config.setTelemetryEnabled(true);
        }
        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
            config.getTelemetryEnabled() ? config.getMachineId() : StringUtils.EMPTY);
        config.setUserAgent(userAgent);
        config.setProduct(CommonConst.PLUGIN_NAME);

    }

    private static void initializeTelemetry() {
        final AzureConfiguration config = Azure.az().config();
        final String version = config.getVersion();
        config.setVersion(CommonConst.PLUGIN_VERSION);
        AppInsightsClient.setAppInsightsConfiguration(new AppInsightsConfigurationImpl());
        if (StringUtils.isBlank(version)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Install, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_INSTALL, null, null);
        } else if (StringUtils.isNotBlank(version) && !AzurePlugin.PLUGIN_VERSION.equalsIgnoreCase(version)) {
            AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Upgrade, null, true);
            EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_UPGRADE, null, null);
        }
        AppInsightsClient.createByType(AppInsightsClient.EventType.Plugin, "", AppInsightsConstants.Load, null, true);
        EventUtil.logEvent(EventType.info, SYSTEM, PLUGIN_LOAD, null, null);
        if (Azure.az().config().getHttpProxy() != null) {
            final Map<String, String> map = Optional.ofNullable(AzureTelemeter.getCommonProperties()).map(HashMap::new).orElse(new HashMap<>());
            map.put(PROXY, "true");
            AzureTelemeter.setCommonProperties(map);
        }
    }

    private static boolean isDataFileValid() {
        final String dataFile = PluginHelper.getTemplateFile(message("dataFileName"));
        final File file = new File(dataFile);
        if (!file.exists()) {
            return false;
        }
        try {
            return ParserXMLUtility.parseXMLFile(dataFile) != null;
        } catch (final Exception e) {
            return false;
        }
    }

}
