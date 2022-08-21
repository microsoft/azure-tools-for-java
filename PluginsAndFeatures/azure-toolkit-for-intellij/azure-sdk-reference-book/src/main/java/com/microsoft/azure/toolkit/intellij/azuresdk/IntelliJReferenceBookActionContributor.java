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
            put("Microsoft.Resources", "Resource Management");
            put("Microsoft.Web", "App Service");
            put("Microsoft.Compute", "Compute");
            put("Microsoft.ContainerRegistry", "Container Registry");
            put("Microsoft.DBforMySQL", "MySQL");
            put("Microsoft.DBforPostgreSQL", "PostgreSQL");
            put("Microsoft.Cache", "Redis");
            put("Microsoft.AppPlatform", "Spring Cloud");
            put("Microsoft.Sql", "SQL");
            put("Microsoft.Storage", "Storage - Blobs");
            put("Microsoft.Insights", "Monitor Ingestion");
            put("Microsoft.DocumentDB", "Cosmos DB");
        }
    };

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AbstractAzService<?, ?>
                || r instanceof AzResourceModule<?, ?, ?> || r instanceof AbstractAzResource<?, ?, ?>;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) ->
                OpenReferenceBookAction.openSdkReferenceBook(e.getProject(), getFeatureFromSource(c));
        am.registerHandler(ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK, condition, handler);
    }

    @Nullable
    private static String getFeatureFromSource(@Nonnull Object source) {
        if (source instanceof AbstractAzService<?, ?>) {
            return SERVICE_FEATURE_MAP.get(((AbstractAzService<?, ?>) source).getFullResourceType());
        } else if (source instanceof AzResourceModule<?, ?, ?>) {
            return SERVICE_FEATURE_MAP.get(((AzResourceModule<?, ?, ?>) source).getFullResourceType());
        } else if (source instanceof AbstractAzResource<?, ?, ?>) {
            return SERVICE_FEATURE_MAP.get(((AbstractAzResource<?, ?, ?>) source).getFullResourceType());
        }
        return null;
    }
}
