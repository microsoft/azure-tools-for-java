/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class StorageAccountResourceDefinition extends AzureServiceResource.Definition<StorageAccount>
    implements SpringSupported<StorageAccount>, FunctionSupported<StorageAccount> {
    public static final StorageAccountResourceDefinition INSTANCE = new StorageAccountResourceDefinition();
    public static final String LOCAL_STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    public static final String CONNECTION_STRING_KEY = String.format("%s_CONNECTION_STRING", Connection.ENV_PREFIX);
    public static final String ACCOUNT_NAME_KEY = String.format("%s_ACCOUNT_NAME", Connection.ENV_PREFIX);
    public static final String ACCOUNT_KEY = String.format("%s_ACCOUNT_KEY", Connection.ENV_PREFIX);

    public StorageAccountResourceDefinition() {
        super("Azure.Storage", "Azure Storage Account", AzureIcons.StorageAccount.MODULE.getIconPath());
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<StorageAccount> accountDef, Project project) {
        final StorageAccount account = accountDef.getData();
        final String conString = account.getConnectionString();
        final HashMap<String, String> env = new HashMap<>();
        env.put(CONNECTION_STRING_KEY, conString);
        env.put(ACCOUNT_NAME_KEY, account.getName());
        env.put(ACCOUNT_KEY, account.getKey());
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        if (StringUtils.containsIgnoreCase(key, "blob")) {
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        } else if (StringUtils.containsIgnoreCase(key, "share")) {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
        } else {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-key", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        }
        return properties;
    }

    @Override
    public Map<String, String> getSpringPropertyTypes() {
        final Map<String, String> fields = new HashMap<>();
        fields.put("spring.cloud.azure.storage.blob.account-name", "Name");
        fields.put("spring.cloud.azure.storage.blob.account-key", "Access Key");
        fields.put("spring.cloud.azure.storage.blob.endpoint", "Endpoint Url");
        fields.put("spring.cloud.azure.storage.fileshare.account-name", "Name");
        fields.put("spring.cloud.azure.storage.fileshare.account-key", "Access Key");
        fields.put("spring.cloud.azure.storage.fileshare.endpoint", "Endpoint Url");
        return fields;
    }

    @Override
    public StorageAccount getResource(String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, AzuriteStorageAccount.AZURITE_RESOURCE_ID)) {
            return AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT;
        }
        return Azure.az(AzureStorageAccount.class).getById(dataId);
    }

    @Override
    public List<Resource<StorageAccount>> getResources(Project project) {
        return Azure.az(AzureStorageAccount.class).list().stream()
            .flatMap(m -> m.storageAccounts().list().stream())
            .map(this::define).collect(Collectors.toList());
    }

    @Override
    public AzureFormJPanel<Resource<StorageAccount>> getResourcePanel(Project project) {
        return new StorageAccountResourcePanel();
    }

    @Nonnull
    @Override
    public String getResourceType() {
        return "Storage";
    }

    @Override
    public List<String> getEnvironmentVariablesKey() {
        return ListUtils.union(Arrays.asList(CONNECTION_STRING_KEY, ACCOUNT_NAME_KEY, ACCOUNT_KEY), super.getEnvironmentVariablesKey());
    }

    @Nullable
    @Override
    public String getResourceConnectionString(@Nonnull StorageAccount resource) {
        return resource instanceof AzuriteStorageAccount ? LOCAL_STORAGE_CONNECTION_STRING : resource.getConnectionString();
    }
}
