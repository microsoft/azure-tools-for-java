/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResourceModule.CONNECTION_STRING_SUBSCRIPTION_ID;

@Getter
public class StorageAccountResourceDefinition extends AzureServiceResource.Definition<IStorageAccount>
    implements SpringSupported<IStorageAccount>, FunctionSupported<IStorageAccount> {
    public static final int METHOD_AZURE = 0;
    public static final int METHOD_AZURITE = 1;
    public static final int METHOD_STRING = 2;

    public static final StorageAccountResourceDefinition INSTANCE = new StorageAccountResourceDefinition();
    public static final String LOCAL_STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";
    public static final String CONNECTION_STRING_KEY = String.format("%s_CONNECTION_STRING", Connection.ENV_PREFIX);
    public static final String ACCOUNT_NAME_KEY = String.format("%s_ACCOUNT_NAME", Connection.ENV_PREFIX);
    public static final String ACCOUNT_KEY = String.format("%s_ACCOUNT_KEY", Connection.ENV_PREFIX);

    @Setter
    private TempData tempData;

    public StorageAccountResourceDefinition() {
        super("Azure.Storage", "Azure Storage Account", AzureIcons.StorageAccount.MODULE.getIconPath());
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<IStorageAccount> accountDef, Project project) {
        final HashMap<String, String> env = new HashMap<>();
        final IStorageAccount account = accountDef.getData();
        if (Objects.nonNull(account)) {
            final String conString = account.getConnectionString();
            env.put(CONNECTION_STRING_KEY, conString);
            env.put(ACCOUNT_NAME_KEY, account.getName());
            env.put(ACCOUNT_KEY, account.getKey());
        }
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
    public IStorageAccount getResource(String dataId, final String id) {
        if (StringUtils.equalsIgnoreCase(dataId, AzuriteStorageAccount.AZURITE_RESOURCE_ID)) {
            return AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT;
        } else if (CONNECTION_STRING_SUBSCRIPTION_ID.equalsIgnoreCase(ResourceId.fromString(dataId).subscriptionId())) {
            final String connectionString = IntelliJSecureStore.getInstance().loadPassword(StorageAccountResourceDefinition.class.getName(), id.toLowerCase(), null);
            if (StringUtils.isNotBlank(connectionString)) {
                return Azure.az(AzureStorageAccount.class).getOrInitByConnectionString(connectionString);
            }
            return null;
        } else {
            return Azure.az(AzureStorageAccount.class).getById(dataId);
        }
    }

    @Override
    public List<Resource<IStorageAccount>> getResources(Project project) {
        return Azure.az(AzureStorageAccount.class).list().stream()
            .flatMap(m -> m.storageAccounts().list().stream())
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<IStorageAccount>> getResourcePanel(Project project) {
        final StorageAccountResourcePanel panel = new StorageAccountResourcePanel();
        if (Objects.nonNull(this.tempData)) {
            panel.setMethod(this.tempData.getType());
            if (StringUtils.isNotBlank(this.tempData.getConnectionString())) {
                final ConnectionStringStorageAccount account = Azure.az(AzureStorageAccount.class).getOrInitByConnectionString(this.tempData.getConnectionString());
                if (Objects.nonNull(account)) {
                    panel.setValue(StorageAccountResourceDefinition.INSTANCE.define(account));
                }
            }
            this.tempData = null;
        }
        return panel;
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
    public String getResourceConnectionString(@Nonnull IStorageAccount resource) {
        return resource instanceof AzuriteStorageAccount ? LOCAL_STORAGE_CONNECTION_STRING : resource.getConnectionString();
    }

    @Data
    @AllArgsConstructor
    public static class TempData {
        private int type; // 0: Azure, 1: Azurite, 2: ConnectionString
        private String connectionString;
    }
}
