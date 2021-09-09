/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.PermanentInstallationID;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.ide.common.util.ParserXMLUtility;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;
import com.microsoft.rest.LogLevel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AzureConfigInitializer {

    public static final String TELEMETRY = "telemetry";
    public static final String ACCOUNT = "account";
    public static final String FUNCTION = "function";
    public static final String DATABASE = "database";

    public static final String TELEMETRY_PLUGIN_VERSION = "telemetry_plugin_version";
    public static final String AZURE_ENVIRONMENT_KEY = "azure_environment";
    public static final String PASSWORD_SAVE_TYPE = "password_save_type";
    public static final String FUNCTION_CORE_TOOLS_PATH = "function_core_tools_path";
    public static final String TELEMETRY_ALLOW_TELEMETRY = "telemetry_allow_telemetry";
    private static final String TELEMETRY_INSTALLATION_ID = "telemetry_installation_id";

    public static void initialize() {
        initializeConfigFromLegacySettings();
        // 1. format valid installation Id
        String installId = AzureStoreManager.getInstance().getMachineStore().getProperty(TELEMETRY,
                TELEMETRY_INSTALLATION_ID);
        if (StringUtils.isBlank(installId) || InstallationIdUtils.isValidHashMac(installId)) {
            // old settings 1
            installId = AzureStoreManager.getInstance().getIdeStore().getProperty(null, TELEMETRY_INSTALLATION_ID);
        }

        if (StringUtils.isBlank(installId) || InstallationIdUtils.isValidHashMac(installId)) {
        }

        if (StringUtils.isBlank(installId) || InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }

        final AzureConfiguration config = Azure.az().config();
        config.setMachineId(installId);

        // new way of saving install id
        AzureStoreManager.getInstance().getMachineStore().setProperty(TELEMETRY,
                TELEMETRY_INSTALLATION_ID, installId);

        final IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();
        final String allowTelemetry = migrateAndGetSetting(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, "true");
        config.setTelemetryEnabled(Boolean.parseBoolean(allowTelemetry));
        ideStore.setProperty(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, config.getTelemetryEnabled().toString());

        final String azureCloud = migrateAndGetSetting(ACCOUNT, AZURE_ENVIRONMENT_KEY, "Azure");
        config.setCloud(azureCloud);
        ideStore.setProperty(ACCOUNT, AZURE_ENVIRONMENT_KEY, azureCloud);
        Azure.az(AzureCloud.class).setByName(config.getCloud());

        final String funcPath = migrateAndGetSetting(FUNCTION, FUNCTION_CORE_TOOLS_PATH, "");
        if (StringUtils.isNotBlank(funcPath) && Files.exists(Paths.get(funcPath))) {
            config.setFunctionCoreToolsPath(funcPath);
        }

        final String passwordSaveType = migrateAndGetSetting(DATABASE, PASSWORD_SAVE_TYPE, "");
        if (StringUtils.isNotBlank(passwordSaveType)) {
            config.setDatabasePasswordSaveType(passwordSaveType);
        }

        migrateAndGetSetting(TELEMETRY, TELEMETRY_PLUGIN_VERSION, "");

        final String userAgent = String.format(AzurePlugin.USER_AGENT, AzurePlugin.PLUGIN_VERSION,
                config.getTelemetryEnabled() ? config.getMachineId() : StringUtils.EMPTY);
        config.setUserAgent(userAgent);
        config.setProduct(CommonConst.PLUGIN_NAME);
        CommonSettings.setUserAgent(config.getUserAgent());
        config.setLogLevel(LogLevel.NONE.name());
        config.setVersion(CommonConst.PLUGIN_VERSION);
    }

    private static void initializeConfigFromLegacySettings() {
        if (isDataFileValid()) {
            // read legacy settings from old data.xml
            try {
                final String dataFile = PluginHelper.getTemplateFile(AzureBundle.message("dataFileName"));
                final boolean allowTelemetry = Boolean.parseBoolean(DataOperations.getProperty(dataFile, AzureBundle.message("prefVal")));
                final String installationId = DataOperations.getProperty(dataFile, AzureBundle.message("instID"));
                final String pluginVersion = DataOperations.getProperty(dataFile, AzureBundle.message("pluginVersion"));

                // check non-empty for valid data.xml
                if (StringUtils.isNoneBlank(installationId, pluginVersion)) {
                    AzureStoreManager.getInstance().getIdeStore().setProperty(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, Boolean.toString(allowTelemetry));
                    AzureStoreManager.getInstance().getIdeStore().setProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION, pluginVersion);
                    if (InstallationIdUtils.isValidHashMac(installationId)) {
                        AzureStoreManager.getInstance().getMachineStore().setProperty(TELEMETRY,
                                TELEMETRY_INSTALLATION_ID, installationId);
                    }
                }
                FileUtils.deleteQuietly(new File(dataFile));
            } catch (Exception ex) {
                final Map<String, String> props = new HashMap<>();
                props.put("error", ex.getMessage());
                EventUtil.logEvent(EventType.error, SYSTEM, TelemetryConstants.PLUGIN_TRANSFER_SETTINGS, props, null);
            }
        }
    }

    private static String migrateAndGetSetting(String service, String key, String defaultValue) {
        final IIdeStore appStore = AzureStoreManager.getInstance().getIdeStore();
        final String legacyValue = appStore.getProperty("", key);
        if (StringUtils.isNotBlank(legacyValue)) {
            appStore.setProperty(service, key, legacyValue);
            appStore.setProperty("", key, null);
            return legacyValue;
        }
        return appStore.getProperty(service, key, defaultValue);
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
