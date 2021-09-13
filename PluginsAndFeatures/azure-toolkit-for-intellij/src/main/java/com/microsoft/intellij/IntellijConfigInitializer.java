/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.application.PermanentInstallationID;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.ide.common.util.ParserXMLUtility;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.azurecommons.xmlhandling.DataOperations;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.messages.AzureBundle;
import com.microsoft.intellij.util.PluginHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer.*;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class IntellijConfigInitializer {

    public static void initialize() {
        initializeConfigFromLegacySettings();
        String installId = AzureStoreManager.getInstance().getIdeStore().getProperty(null, TELEMETRY_INSTALLATION_ID);

        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.getHashMac();
        }
        if (StringUtils.isBlank(installId) || !InstallationIdUtils.isValidHashMac(installId)) {
            installId = InstallationIdUtils.hash(PermanentInstallationID.get());
        }

        migrateLegacySetting(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, "true");
        migrateLegacySetting(ACCOUNT, AZURE_ENVIRONMENT_KEY, "Azure");
        migrateLegacySetting(FUNCTION, FUNCTION_CORE_TOOLS_PATH, "");
        migrateLegacySetting(DATABASE, PASSWORD_SAVE_TYPE, "");
        migrateLegacySetting(TELEMETRY, TELEMETRY_PLUGIN_VERSION, "");

        AzureConfigInitializer.initialize(installId, "Azure Toolkit for IntelliJ", AzurePlugin.PLUGIN_VERSION);
        CommonSettings.setUserAgent(Azure.az().config().getUserAgent());
        if (StringUtils.isNotBlank(Azure.az().config().getCloud())) {
            Azure.az(AzureCloud.class).setByName(Azure.az().config().getCloud());
        }
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
                    AzureStoreManager.getInstance().getIdeStore().setProperty(TELEMETRY,
                            TELEMETRY_ALLOW_TELEMETRY, Boolean.toString(allowTelemetry));
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

    private static void migrateLegacySetting(String service, String key, String defaultValue) {
        final IIdeStore appStore = AzureStoreManager.getInstance().getIdeStore();
        final String legacyValue = appStore.getProperty("", key);
        if (StringUtils.isNotBlank(legacyValue)) {
            appStore.setProperty(service, key, legacyValue);
            appStore.setProperty("", key, null);
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
