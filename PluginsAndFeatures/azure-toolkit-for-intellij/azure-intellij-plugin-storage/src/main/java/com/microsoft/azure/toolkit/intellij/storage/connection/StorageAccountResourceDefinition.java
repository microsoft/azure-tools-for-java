/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
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
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class StorageAccountResourceDefinition extends AzureServiceResource.Definition<StorageAccount>
        implements SpringSupported<StorageAccount>, FunctionSupported<StorageAccount> {
    public static final StorageAccountResourceDefinition INSTANCE = new StorageAccountResourceDefinition();

    public StorageAccountResourceDefinition() {
        super("Azure.Storage", "Azure Storage Account", AzureIcons.StorageAccount.MODULE.getIconPath());
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<StorageAccount> accountDef, Project project) {
        final StorageAccount account = accountDef.getData();
        final String conString = account.getConnectionString();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_CONNECTION_STRING", Connection.ENV_PREFIX), conString);
        env.put(String.format("%s_ACCOUNT_NAME", Connection.ENV_PREFIX), account.name());
        env.put(String.format("%s_ACCOUNT_KEY", Connection.ENV_PREFIX), account.getKey());
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        final String suffix = Azure.az(AzureCloud.class).get().getStorageEndpointSuffix();
        properties.add(Pair.of("azure.storage.accountName", String.format("${%s_ACCOUNT_NAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("azure.storage.accountKey", String.format("${%s_ACCOUNT_KEY}", Connection.ENV_PREFIX)));
        final String blobEndpoint = "https://${%s_ACCOUNT_NAME}.blob%s/<your-container-name>/<your-blob-name>";
        final String fileEndpoint = "https://${%s_ACCOUNT_NAME}.file%s/<your-fileshare-name>/<your-file-name>";
        properties.add(Pair.of("# azure.storage.blob-endpoint", String.format(blobEndpoint, Connection.ENV_PREFIX, suffix)));
        properties.add(Pair.of("# azure.storage.file-endpoint", String.format(fileEndpoint, Connection.ENV_PREFIX, suffix)));
        return properties;
    }

    @Override
    public StorageAccount getResource(String dataId) {
        final ResourceId resourceId = ResourceId.fromString(dataId);
        final String subscriptionId = resourceId.subscriptionId();
        final String rg = resourceId.resourceGroupName();
        final String name = resourceId.name();
        return Azure.az(AzureStorageAccount.class).forSubscription(subscriptionId).storageAccounts().get(name, rg);
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

    @Nullable
    @Override
    public String getResourceConnectionString(@Nonnull StorageAccount resource) {
        return resource.getConnectionString();
    }
}
