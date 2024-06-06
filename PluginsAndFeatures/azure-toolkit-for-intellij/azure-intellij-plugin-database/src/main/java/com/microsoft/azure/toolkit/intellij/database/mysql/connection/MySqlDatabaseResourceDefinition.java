/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.database.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResourceDefinition;
import com.microsoft.azure.toolkit.intellij.database.connection.SqlDatabaseResourcePanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.MySqlDatabase;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class MySqlDatabaseResourceDefinition extends SqlDatabaseResourceDefinition<MySqlDatabase> {
    public static final MySqlDatabaseResourceDefinition INSTANCE = new MySqlDatabaseResourceDefinition();

    public MySqlDatabaseResourceDefinition() {
        super("Azure.MySQL", "Azure Database for MySQL", AzureIcons.MySQL.MODULE.getIconPath());
    }

    @Override
    public MySqlDatabase getResource(String dataId, final String id) {
        return Azure.az(AzureMySql.class).getById(dataId);
    }

    @Override
    public List<Resource<MySqlDatabase>> getResources(Project project) {
        return Azure.az(AzureMySql.class).list().stream()
            .flatMap(m -> m.servers().list().stream())
            .flatMap(s -> {
                try {
                    return s.databases().list().stream();
                } catch (final Throwable e) {
                    return Stream.empty();
                }
            })
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<MySqlDatabase>> getResourcePanel(Project project) {
        return new SqlDatabaseResourcePanel<>(this) {
            @Override
            protected ServerComboBox<IDatabaseServer<MySqlDatabase>> initServerComboBox() {
                return new ServerComboBox<>() {
                    @Nonnull
                    @Override
                    protected List<? extends IDatabaseServer<MySqlDatabase>> loadItems() {
                        return Optional.ofNullable(this.getSubscription())
                            .map(s -> Azure.az(AzureMySql.class).servers(s.getId()).list())
                            .orElse(Collections.emptyList());
                    }

                    @Override
                    protected void refreshItems() {
                        Optional.ofNullable(this.getSubscription())
                            .ifPresent(s -> Azure.az(AzureMySql.class).servers(s.getId()).refresh());
                        super.refreshItems();
                    }
                };
            }
        };
    }
}

