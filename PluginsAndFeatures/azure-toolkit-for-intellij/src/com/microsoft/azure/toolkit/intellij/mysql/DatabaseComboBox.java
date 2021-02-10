/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerState;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<DatabaseInner> {

    @Setter
    private Subscription subscription;
    private Server server;

    public DatabaseComboBox() {
        super(false);
    }

    public void setServer(Server server) {
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
        if (item instanceof DatabaseInner) {
            return ((DatabaseInner) item).name();
        }
        return super.getItemText(item);
    }

    @Override
    protected List<? extends DatabaseInner> loadItems() throws Exception {
        if (Objects.isNull(subscription) || Objects.isNull(server) || !ServerState.READY.equals(server.userVisibleState())) {
            return new ArrayList<>();
        }
        return MySQLMvpModel.DatabaseMvpModel.listDatabases(subscription.subscriptionId(), server);
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    protected String label() {
        return "MySQL Database Sever";
    }
}
