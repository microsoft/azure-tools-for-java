/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.store;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import org.apache.commons.lang3.StringUtils;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    public static final String TELEMETRY_INSTALLATION_ID = "telemetry_installation_id";

    public static void initialize(String defaultMachineId, String pluginName, String pluginVersion) {
        String machineId = AzureStoreManager.getInstance().getMachineStore().getProperty(TELEMETRY,
                TELEMETRY_INSTALLATION_ID);
        if (StringUtils.isBlank(machineId) || !InstallationIdUtils.isValidHashMac(machineId)) {
            machineId = defaultMachineId;
        }

        final AzureConfiguration config = Azure.az().config();
        config.setMachineId(machineId);

        final IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();
        final String allowTelemetry = ideStore.getProperty(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, "true");
        config.setTelemetryEnabled(Boolean.parseBoolean(allowTelemetry));

        final String azureCloud = ideStore.getProperty(ACCOUNT, AZURE_ENVIRONMENT_KEY, "Azure");
        config.setCloud(azureCloud);

        final String funcPath = ideStore.getProperty(FUNCTION, FUNCTION_CORE_TOOLS_PATH, "");
        if (StringUtils.isNotBlank(funcPath) && Files.exists(Paths.get(funcPath))) {
            config.setFunctionCoreToolsPath(funcPath);
        }

        final String passwordSaveType = ideStore.getProperty(DATABASE, PASSWORD_SAVE_TYPE, "");
        if (StringUtils.isNotBlank(passwordSaveType)) {
            config.setDatabasePasswordSaveType(passwordSaveType);
        }

        ideStore.getProperty(TELEMETRY, TELEMETRY_PLUGIN_VERSION, "");

        final String userAgent = String.format("%s, v%s, machineid:%s", pluginName, pluginVersion,
                config.getTelemetryEnabled() ? config.getMachineId() : StringUtils.EMPTY);
        config.setUserAgent(userAgent);
        config.setProduct(pluginName);
        config.setLogLevel("NONE");
        config.setVersion(pluginVersion);
        saveAzConfig();
    }

    public static void saveAzConfig() {
        final AzureConfiguration config = Azure.az().config();
        IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();

        AzureStoreManager.getInstance().getMachineStore().setProperty(TELEMETRY, TELEMETRY_INSTALLATION_ID,
                config.getMachineId());

        ideStore.setProperty(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, Boolean.toString(config.getTelemetryEnabled()));
        ideStore.setProperty(ACCOUNT, AZURE_ENVIRONMENT_KEY, config.getCloud());
        ideStore.setProperty(FUNCTION, FUNCTION_CORE_TOOLS_PATH, config.getFunctionCoreToolsPath());
        ideStore.setProperty(DATABASE, PASSWORD_SAVE_TYPE, config.getDatabasePasswordSaveType());
        // don't save pluginVersion, it is saved in AzurePlugin class
    }
}
