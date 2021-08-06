/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sqlserver.common;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TODO: refactor together with DatabaseComboBox.
 */
public class SqlServerDatabaseComboBox extends AzureComboBox<SqlDatabaseEntity> {

    private ISqlServer server;

    public void setServer(ISqlServer server) {
        if (Objects.equals(server, this.server)) {
            return;
        }
        this.server = server;
        if (server == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof SqlDatabaseEntity) {
            return ((SqlDatabaseEntity) item).getName();
        }
        return super.getItemText(item);
    }

    @Override
    @AzureOperation(
        name = "sqlserver|server.database_load|subscription",
        params = {"this.server.name()", "this.subscription.subscriptionId()"},
        type = AzureOperation.Type.SERVICE
    )
    @Nonnull
    protected List<? extends SqlDatabaseEntity> loadItems() throws Exception {
        if (Objects.isNull(server)) {
            return new ArrayList<>();
        }
        return server.databases();
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
