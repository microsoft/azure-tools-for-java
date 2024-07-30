/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.keyvault.AzureKeyVault;
import com.microsoft.azure.toolkit.lib.keyvault.KeyVault;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class BaseKeyVaultResourceDefinition extends AzureServiceResource.Definition<KeyVault> {

    public BaseKeyVaultResourceDefinition() {
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
    public KeyVault getResource(String dataId, final String id) {
        return Azure.az(AzureKeyVault.class).getById(dataId);
    }

    @Override
    public List<Resource<KeyVault>> getResources(Project project) {
        return Azure.az(AzureKeyVault.class).list().stream()
            .flatMap(m -> m.getKeyVaultModule().list().stream())
            .map(this::define).toList();
    }
}
