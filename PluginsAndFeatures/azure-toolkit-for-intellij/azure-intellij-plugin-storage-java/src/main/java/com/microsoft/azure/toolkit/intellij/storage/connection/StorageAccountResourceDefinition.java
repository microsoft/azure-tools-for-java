/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AuthenticationType;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringManagedIdentitySupported;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;


@Getter
public class StorageAccountResourceDefinition extends BaseStorageAccountResourceDefinition
    implements SpringManagedIdentitySupported<IStorageAccount> {
    public static final String CLIENT_ID_KEY = String.format("%s_CLIENT_ID", Connection.ENV_PREFIX);

    public static final StorageAccountResourceDefinition INSTANCE = new StorageAccountResourceDefinition();

    @Setter
    private TempData tempData;

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
    public Map<String, String> initIdentityEnv(Connection<IStorageAccount, ?> connection, Project project) {
        final HashMap<String, String> env = new HashMap<>();
        final Resource<IStorageAccount> accountDef = connection.getResource();
        final IStorageAccount account = accountDef.getData();
        if (Objects.nonNull(account)) {
            env.put(ACCOUNT_NAME_KEY, account.getName());
        }
        if (connection.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            Optional.ofNullable(connection.getUserAssignedManagedIdentity()).map(Resource::getData).ifPresent(identity -> {
                env.put(String.format("%s_CLIENT_ID", Connection.ENV_PREFIX), identity.getClientId());
                env.put("AZURE_CLIENT_ID", identity.getClientId());
            });
        }
        return env;
    }

    @Nullable
    @Override
    public Map<String, BuiltInRole> getBuiltInRoles() {
        return Map.of("ba92f5b4-2d11-453d-a403-e96b0029c9fe", BuiltInRole.STORAGE_BLOB_DATA_CONTRIBUTOR,
                "974c5e8b-45b9-4653-ba55-5f855dd0fb88", BuiltInRole.STORAGE_QUEUE_DATA_CONTRIBUTOR,
                "0c867c2a-1d8c-454a-a3db-ab2ea1bdc8bb", BuiltInRole.STORAGE_FILE_DATA_SMB_SHARE_CONTRIBUTOR);
    }

    @Override
    public List<String> getRequiredPermissions() {
        return Arrays.asList(
                "Microsoft.Storage/storageAccounts/blobServices/containers/delete",
                "Microsoft.Storage/storageAccounts/blobServices/containers/read",
                "Microsoft.Storage/storageAccounts/blobServices/containers/write",
                "Microsoft.Storage/storageAccounts/blobServices/generateUserDelegationKey/action",
                "Microsoft.Storage/storageAccounts/queueServices/queues/delete",
                "Microsoft.Storage/storageAccounts/queueServices/queues/read",
                "Microsoft.Storage/storageAccounts/queueServices/queues/write"
        );
    }

    @Override
    public List<Pair<String, String>> getSpringPropertiesForManagedIdentity(String key, Connection<?,?> connection) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        if (StringUtils.containsIgnoreCase(key, "blob")) {
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        } else if (StringUtils.containsIgnoreCase(key, "share")) {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
        } else {
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.fileshare.endpoint", String.format("https://${%s_ACCOUNT_NAME}.file%s", Connection.ENV_PREFIX, suffix)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.account-name", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.storage.blob.endpoint", String.format("https://${%s_ACCOUNT_NAME}.blob%s", Connection.ENV_PREFIX, suffix)));
        }
        // properties.add(Pair.of("spring.cloud.azure.storage.blob.credential.managed-identity-enabled", String.valueOf(Boolean.TRUE)));
        // for user assigned managed identity
        if (connection.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            properties.add(Pair.of("spring.cloud.azure.storage.blob.credential.client-id", String.format("${%s_CLIENT_ID}", Connection.ENV_PREFIX)));
        }
        return properties;
    }

    @Override
    public List<AuthenticationType> getSupportedAuthenticationTypes(Resource<?> resource) {
        final Object data = resource.getData();
        return data instanceof ConnectionStringStorageAccount || data instanceof AzuriteStorageAccount ?
                Collections.singletonList(AuthenticationType.CONNECTION_STRING) :
                super.getSupportedAuthenticationTypes(resource);
    }

    @Data
    @AllArgsConstructor
    public static class TempData {
        private int type; // 0: Azure, 1: Azurite, 2: ConnectionString
        private String connectionString;
    }
}
