/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.sql;

import com.intellij.openapi.application.PreloadingActivity;
import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResourceConnection;
import org.jetbrains.annotations.NotNull;

public class SqlServerConnectionDefinitionPreloadingActivity extends PreloadingActivity {

    @Override
    public void preload(@NotNull ProgressIndicator progressIndicator) {
        final String resourceType = SqlServerDatabaseResource.Definition.SQL_SERVER.getType();
        final String consumerType = ModuleResource.TYPE;
        ResourceManager.registerDefinition(SqlServerDatabaseResource.Definition.SQL_SERVER);
        ConnectionManager.registerDefinition(resourceType, consumerType, DatabaseResourceConnection.Definition.MODULE_SQL);
    }
}
