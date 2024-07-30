/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AuthenticationType;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringManagedIdentitySupported;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;


@Getter
public class KeyVaultResourceDefinition extends BaseKeyVaultResourceDefinition
        implements SpringSupported<KeyVault>, SpringManagedIdentitySupported<KeyVault> {

    public static final KeyVaultResourceDefinition INSTANCE = new KeyVaultResourceDefinition();

    @Override
    public AzureFormJPanel<Resource<KeyVault>> getResourcePanel(Project project) {
        return new KeyVaultResourcePanel();
    }

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        if (StringUtils.containsIgnoreCase(key, "certificate")) {
            properties.add(Pair.of("spring.cloud.azure.keyvault.certificate.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
        } else if (StringUtils.containsIgnoreCase(key, "secret")) {
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-source-enabled", "true"));
        } else {
            properties.add(Pair.of("spring.cloud.azure.keyvault.certificate.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
            properties.add(Pair.of("spring.cloud.azure.keyvault.secret.property-source-enabled", "true"));
        }
        return properties;
    }

    @Override
    public List<Pair<String, String>> getSpringPropertiesForManagedIdentity(String key, Connection<?, ?> connection) {
        final ArrayList<Pair<String, String>> result = new ArrayList<>(getSpringProperties(key));
        result.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].credential.managed-identity-enabled", String.valueOf(Boolean.TRUE)));
        if (connection.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            result.add(Pair.of("spring.cloud.azure.keyvault.secret.property-sources[0].credential.client-id", String.format("${%s_CLIENT_ID}", Connection.ENV_PREFIX)));
        }
        return result;
    }

    @Override
    public Map<String, String> initIdentityEnv(Connection<KeyVault, ?> data, Project project) {
        final KeyVault vault = data.getResource().getData();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_ENDPOINT", Connection.ENV_PREFIX), vault.getVaultUri());
        if (data.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            Optional.ofNullable(data.getUserAssignedManagedIdentity()).map(Resource::getData)
                    .ifPresent(identity -> env.put(String.format("%s_CLIENT_ID", Connection.ENV_PREFIX), identity.getClientId()));
        }
        return env;
    }

    @Override
    public List<String> getRequiredPermissions() {
        return List.of(
                "Microsoft.KeyVault/vaults/secrets/getSecret/action",
                "Microsoft.KeyVault/vaults/secrets/readMetadata/action",
                "Microsoft.KeyVault/vaults/keys/read"
        );
    }

    @Nullable
    @Override
    public Map<String, BuiltInRole> getBuiltInRoles() {
        return Map.of(
                "4633458b-17de-408a-b874-0445c86b69e6", BuiltInRole.KEY_VAULT_SECRETS_USER,
                "12338af0-0e69-4776-bea7-57ae8d297424", BuiltInRole.KEY_VAULT_CRYPTO_USER
        );
    }
}
