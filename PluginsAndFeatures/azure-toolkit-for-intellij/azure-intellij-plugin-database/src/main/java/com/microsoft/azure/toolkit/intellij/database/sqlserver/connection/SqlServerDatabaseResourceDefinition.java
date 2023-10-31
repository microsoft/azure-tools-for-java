/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.database.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResourcePanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlDatabase;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SqlServerDatabaseResourceDefinition extends SqlDatabaseResourceDefinition<MicrosoftSqlDatabase> {
    public static final SqlServerDatabaseResourceDefinition INSTANCE = new SqlServerDatabaseResourceDefinition();

    public SqlServerDatabaseResourceDefinition() {
        super("Azure.SqlServer", "SQL Server", AzureIcons.SqlServer.MODULE.getIconPath());
    }

    @Override
    public MicrosoftSqlDatabase getResource(String dataId) {
        return Azure.az(AzureSqlServer.class).getById(dataId);
    }

    @Override
    public List<Resource<MicrosoftSqlDatabase>> getResources(Project project) {
        return Azure.az(AzureSqlServer.class).list().stream()
            .flatMap(m -> m.servers().list().stream())
            .flatMap(s -> s.databases().list().stream())
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<MicrosoftSqlDatabase>> getResourcePanel(Project project) {
        return new SqlDatabaseResourcePanel<>(this) {
            @Override
            protected ServerComboBox<IDatabaseServer<MicrosoftSqlDatabase>> initServerComboBox() {
                return new ServerComboBox<>() {
                    @Nonnull
                    @Override
                    protected List<? extends IDatabaseServer<MicrosoftSqlDatabase>> loadItems() {
                        return Optional.ofNullable(this.getSubscription())
                            .map(s -> Azure.az(AzureSqlServer.class).servers(s.getId()).list())
                            .orElse(Collections.emptyList());
                    }

                    @Override
                    protected void refreshItems() {
                        Optional.ofNullable(this.getSubscription())
                            .ifPresent(s -> Azure.az(AzureSqlServer.class).servers(s.getId()).refresh());
                        super.refreshItems();
                    }
                };
            }
        };
    }
}

