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
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.dotnet.DotnetRuntimeHandler.getDotnetRuntimePath;

public class AzureConfigInitializer {
    public static final String TELEMETRY = "telemetry";
    public static final String COMMON = "common";
    public static final String ACCOUNT = "account";
    public static final String FUNCTION = "function";
    public static final String STORAGE = "storage";
    public static final String COSMOS = "cosmos";
    public static final String BICEP = "bicep";
    public static final String MONITOR = "monitor";
    public static final String AZURITE = "azurite";
    public static final String EVENT_HUBS = "event_hubs";
    public static final String OTHER = "other";

    public static final String PAGE_SIZE = "page_size";
    public static final String TELEMETRY_PLUGIN_VERSION = "telemetry_plugin_version";
    public static final String AZURE_ENVIRONMENT_KEY = "azure_environment";
    public static final String FUNCTION_CORE_TOOLS_PATH = "function_core_tools_path";
    public static final String TELEMETRY_ALLOW_TELEMETRY = "telemetry_allow_telemetry";
    public static final String TELEMETRY_INSTALLATION_ID = "telemetry_installation_id";
    public static final String STORAGE_EXPLORER_PATH = "storage_explorer_path";
    public static final String DOCUMENTS_LABEL_FIELDS = "documents_label_fields";
    public static final String DOTNET_RUNTIME_PATH = "dotnet_runtime_path";
    public static final String ENABLE_AUTH_PERSISTENCE = "enable_auth_persistence";
    public static final String MONITOR_TABLE_ROWS = "monitor_table_rows";
    public static final String CONSUMER_GROUP_NAME = "consumer_group_name";
    public static final String AZURITE_PATH = "azurite_path";
    public static final String AZURE_CLI_PATH = "azure_cli_path";
    public static final String AZURITE_WORKSPACE = "azurite_workspace";
    public static final String ENABLE_LEASE_MODE = "enable_lease_mode";

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
        final String enableAuthPersistence = ideStore.getProperty(OTHER, ENABLE_AUTH_PERSISTENCE, "true");
        config.setAuthPersistenceEnabled(Boolean.parseBoolean(enableAuthPersistence));

        final String azureCloud = ideStore.getProperty(ACCOUNT, AZURE_ENVIRONMENT_KEY, "Azure");
        config.setCloud(azureCloud);

        final String funcPath = ideStore.getProperty(FUNCTION, FUNCTION_CORE_TOOLS_PATH, "");
        if (StringUtils.isNotBlank(funcPath) && Files.exists(Paths.get(funcPath))) {
            config.setFunctionCoreToolsPath(funcPath);
        }

        final String cliPath = ideStore.getProperty(COMMON, AZURE_CLI_PATH, "");
        if (StringUtils.isNotBlank(cliPath) && Files.exists(Paths.get(cliPath))) {
            config.setAzureCliPath(cliPath);
        }

        final String storageExplorerPath = ideStore.getProperty(STORAGE, STORAGE_EXPLORER_PATH, "");
        if (StringUtils.isNoneBlank(storageExplorerPath)) {
            config.setStorageExplorerPath(storageExplorerPath);
        }

        final String pageSize = ideStore.getProperty(COMMON, PAGE_SIZE, "99");
        if (StringUtils.isNotEmpty(pageSize)) {
            config.setPageSize(Integer.parseInt(pageSize));
        }

        final String monitorRows = ideStore.getProperty(MONITOR, MONITOR_TABLE_ROWS, "200");
        if (StringUtils.isNotEmpty(monitorRows)) {
            config.setMonitorQueryRowNumber(Integer.parseInt(monitorRows));
        }

        final String defaultDocumentsLabelFields = String.join(";", AzureConfiguration.DEFAULT_DOCUMENT_LABEL_FIELDS);
        final String documentsLabelFields = ideStore.getProperty(COSMOS, DOCUMENTS_LABEL_FIELDS, defaultDocumentsLabelFields);
        if (StringUtils.isNoneBlank(documentsLabelFields)) {
            config.setDocumentsLabelFields(Arrays.stream(documentsLabelFields.split(";")).collect(Collectors.toList()));
        }

        final String defaultDotnetRuntimePath = getDotnetRuntimePath();
        final String dotnetRuntimePath = ideStore.getProperty(BICEP, DOTNET_RUNTIME_PATH, defaultDotnetRuntimePath);
        if (StringUtils.isNoneBlank(dotnetRuntimePath)) {
            config.setDotnetRuntimePath(dotnetRuntimePath);
        }

        final String consumerGroupName = ideStore.getProperty(EVENT_HUBS, CONSUMER_GROUP_NAME, "$Default");
        if (StringUtils.isNotBlank(consumerGroupName)) {
            config.setEventHubsConsumerGroup(consumerGroupName);
        }

        final String azuritePath = ideStore.getProperty(AZURITE, AZURITE_PATH, "");
        if (StringUtils.isNotBlank(azuritePath)) {
            config.setAzuritePath(azuritePath);
        }

        final String azuriteWorkspace = ideStore.getProperty(AZURITE, AZURITE_WORKSPACE, "");
        if (StringUtils.isNotBlank(azuriteWorkspace)) {
            config.setAzuriteWorkspace(azuriteWorkspace);
        }

        final Boolean enableLeaseMode = Boolean.valueOf(ideStore.getProperty(AZURITE, ENABLE_LEASE_MODE, "false"));
        config.setEnableLeaseMode(enableLeaseMode);

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
        final IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();

        AzureStoreManager.getInstance().getMachineStore().setProperty(TELEMETRY, TELEMETRY_INSTALLATION_ID,
            config.getMachineId());

        ideStore.setProperty(TELEMETRY, TELEMETRY_ALLOW_TELEMETRY, Boolean.toString(config.getTelemetryEnabled()));
        ideStore.setProperty(OTHER, ENABLE_AUTH_PERSISTENCE, Boolean.toString(config.isAuthPersistenceEnabled()));
        ideStore.setProperty(MONITOR, MONITOR_TABLE_ROWS, String.valueOf(config.getMonitorQueryRowNumber()));
        ideStore.setProperty(ACCOUNT, AZURE_ENVIRONMENT_KEY, config.getCloud());
        ideStore.setProperty(FUNCTION, FUNCTION_CORE_TOOLS_PATH, config.getFunctionCoreToolsPath());
        ideStore.setProperty(STORAGE, STORAGE_EXPLORER_PATH, config.getStorageExplorerPath());
        ideStore.setProperty(COMMON, PAGE_SIZE, String.valueOf(config.getPageSize()));
        ideStore.setProperty(COMMON, AZURE_CLI_PATH, String.valueOf(config.getAzureCliPath()));
        ideStore.setProperty(COSMOS, DOCUMENTS_LABEL_FIELDS, String.join(";", config.getDocumentsLabelFields()));
        // don't save pluginVersion, it is saved in AzurePlugin class
        ideStore.setProperty(BICEP, DOTNET_RUNTIME_PATH, config.getDotnetRuntimePath());
        ideStore.setProperty(EVENT_HUBS, CONSUMER_GROUP_NAME, config.getEventHubsConsumerGroup());
        ideStore.setProperty(AZURITE, AZURITE_PATH, config.getAzuritePath());
        ideStore.setProperty(AZURITE, AZURITE_WORKSPACE, config.getAzuriteWorkspace());
        ideStore.setProperty(AZURITE, ENABLE_LEASE_MODE, String.valueOf(config.getEnableLeaseMode()));
    }
}
