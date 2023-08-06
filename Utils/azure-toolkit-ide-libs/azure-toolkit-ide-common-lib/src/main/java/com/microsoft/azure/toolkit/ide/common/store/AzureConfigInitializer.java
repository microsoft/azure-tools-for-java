/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.store;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
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
    public static final String AZURITE_WORKSPACE = "azurite_workspace";
    public static final String ENABLE_LEASE_MODE = "enable_lease_mode";
    public static final String SYSTEM = "system";
    public static final String AZURE_CONFIGURATION = "azure_configuration";

    public static void initialize(String defaultMachineId, String pluginName, String pluginVersion) {
        final String machineId = Optional.ofNullable(AzureStoreManager.getInstance().getMachineStore().getProperty(TELEMETRY, TELEMETRY_INSTALLATION_ID))
                .filter(StringUtils::isNotBlank)
                .filter(InstallationIdUtils::isValidHashMac)
                .orElse(defaultMachineId);
        final AzureConfiguration config = Azure.az().config();
        config.setMachineId(machineId);
        config.setProduct(pluginName);
        config.setVersion(pluginVersion);
        config.setLogLevel("NONE");
        final String userAgent = String.format("%s, v%s, machineid:%s", pluginName, pluginVersion,
                BooleanUtils.isNotFalse(config.getTelemetryEnabled()) ? config.getMachineId() : StringUtils.EMPTY);
        config.setUserAgent(userAgent);

        final IIdeStore ideStore = AzureStoreManager.getInstance().getIdeStore();
        final String property = ideStore.getProperty(SYSTEM, AZURE_CONFIGURATION);
        if (StringUtils.isBlank(property)) {
            loadLegacyData(ideStore, config);
        } else {
            final AzureConfiguration azureConfiguration = JsonUtils.fromJson(property, AzureConfiguration.class);
            try {
                Utils.copyProperties(config, azureConfiguration, false);
            } catch (IllegalAccessException e) {
                AzureMessager.getMessager().warning("Failed to load azure configuration from store.", e);
            }
        }
        saveAzConfig();
    }

    private static void loadLegacyData(final IIdeStore ideStore, final AzureConfiguration config) {
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
    }

    public static void saveAzConfig() {
        final AzureConfiguration config = Azure.az().config();
        final AzureStoreManager storeManager = AzureStoreManager.getInstance();
        storeManager.getIdeStore().setProperty(SYSTEM, AZURE_CONFIGURATION, JsonUtils.toJson(config));
        storeManager.getMachineStore().setProperty(TELEMETRY, TELEMETRY_INSTALLATION_ID, config.getMachineId());
    }
}
