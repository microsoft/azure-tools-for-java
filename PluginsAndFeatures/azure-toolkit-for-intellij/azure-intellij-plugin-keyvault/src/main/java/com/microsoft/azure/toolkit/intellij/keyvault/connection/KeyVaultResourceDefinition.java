/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class KeyVaultResourceDefinition extends AzureServiceResource.Definition<KeyVault> implements SpringSupported<KeyVault> {
    public static final KeyVaultResourceDefinition INSTANCE = new KeyVaultResourceDefinition();

    public KeyVaultResourceDefinition() {
        super("Microsoft.KeyVault", "Azure Key Vault", AzureIcons.KeyVault.MODULE.getIconPath());
    }

    @Override
    public String getDefaultEnvPrefix() {
        return "KEY_VAULT";
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<KeyVault> vaultDef, Project project) {
        final KeyVault vault = vaultDef.getData();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_ENDPOINT", Connection.ENV_PREFIX), vault.getVaultUri());
        return env;
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
    public KeyVault getResource(String dataId, final String id) {
        return Azure.az(AzureKeyVault.class).getById(dataId);
    }

    @Override
    public List<Resource<KeyVault>> getResources(Project project) {
        return Azure.az(AzureKeyVault.class).list().stream()
            .flatMap(m -> m.getKeyVaultModule().list().stream())
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<KeyVault>> getResourcePanel(Project project) {
        return new KeyVaultResourcePanel();
    }
}
