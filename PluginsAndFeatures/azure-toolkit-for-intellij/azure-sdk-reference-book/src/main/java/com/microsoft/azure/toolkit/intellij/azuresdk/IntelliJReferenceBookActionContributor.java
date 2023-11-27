/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.OpenReferenceBookAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntelliJReferenceBookActionContributor implements IActionsContributor {
    private static Map<String, String> SERVICE_FEATURE_MAP = new HashMap<>() {
        {
            put("Microsoft.ContainerService", "Container Service");
            put("Microsoft.ContainerService/managedClusters", "Container Service");
            put("Microsoft.Resources", "Core - Parent");
            put("Microsoft.Resources/resourceGroups", "Core - Parent");
            put("Microsoft.Web", "App Service");
            put("Microsoft.Web/sites", "App Service");
            put("Microsoft.Compute", "Compute");
            put("Microsoft.Compute/virtualMachines", "Compute");
            put("Microsoft.ContainerRegistry", "Container Registry");
            put("Microsoft.ContainerRegistry/registries", "Container Registry");
            put("Microsoft.DBforMySQL", "Database for MySQL");
            put("Microsoft.DBforMySQL/servers", "Database for MySQL");
            put("Microsoft.DBforMySQL/flexibleServers", "Database for MySQL");
            put("Microsoft.DBforPostgreSQL", "Database for PostgreSQL");
            put("Microsoft.DBforPostgreSQL/servers", "Database for PostgreSQL");
            put("Microsoft.DBforPostgreSQL/flexibleServers", "Database for PostgreSQL");
            put("Microsoft.Cache", "Redis");
            put("Microsoft.Cache/Redis", "Redis");
            put("Microsoft.AppPlatform", "App Platform");
            put("Microsoft.AppPlatform/Spring", "App Platform");
            put("Microsoft.Sql", "SQL");
            put("Microsoft.Sql/servers", "SQL");
            put("Microsoft.Storage", "Storage - Blobs");
            put("Microsoft.Storage/storageAccounts", "Storage - Blobs");
            put("Microsoft.Insights", "Monitor Ingestion");
            put("Microsoft.Insights/components", "Monitor Ingestion");
            put("Microsoft.DocumentDB", "Cosmos DB");
            put("Microsoft.DocumentDB/databaseAccounts", "Cosmos DB");
            put("Microsoft.HDInsight", "HDInsight");
            put("Microsoft.HDInsight/clusters", "HDInsight");
            put("Microsoft.EventHub", "Event Hubs");
            put("Microsoft.EventHub/namespaces", "Event Hubs");
            put("Microsoft.ServiceBus", "Service Bus");
            put("Microsoft.ServiceBus/namespaces", "Service Bus");
            put("Microsoft.CognitiveServices", "OpenAI");
            put("Microsoft.CognitiveServices/accounts", "OpenAI");
            put("Microsoft.KeyVault", "Key Vault");
            put("Microsoft.KeyVault/vaults", "Key Vault");
            put("Microsoft.KeyVault/vaults/keys", "Key Vault - Keys");
            put("Microsoft.KeyVault/vaults/secrets", "Key Vault - Secrets");
            put("Microsoft.KeyVault/vaults/certificates", "Key Vault - Certificates");
        }
    };

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AbstractAzService<?, ?>
                || r instanceof AzResourceModule<?> || r instanceof AbstractAzResource<?, ?, ?>;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) ->
                OpenReferenceBookAction.openSdkReferenceBook(e.getProject(), getFeatureFromSource(c));
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK, condition, handler);
    }

    @Nullable
    private static String getFeatureFromSource(@Nonnull Object source) {
        if (source instanceof AbstractAzService<?, ?>) {
            return SERVICE_FEATURE_MAP.get(((AbstractAzService<?, ?>) source).getFullResourceType());
        } else if (source instanceof AzResourceModule<?>) {
            return SERVICE_FEATURE_MAP.get(((AzResourceModule<?>) source).getFullResourceType());
        } else if (source instanceof AbstractAzResource<?, ?, ?>) {
            return SERVICE_FEATURE_MAP.get(((AbstractAzResource<?, ?, ?>) source).getFullResourceType());
        }
        return null;
    }
}
