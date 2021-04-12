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
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<DatabaseInner> {

    private Subscription subscription;
    private Server server;

    public DatabaseComboBox() {
        super(false);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
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
    @AzureOperation(
        name = "mysql|database.list.server|subscription",
        params = {"@server.name()", "@subscription.subscriptionId()"},
        type = AzureOperation.Type.SERVICE
    )
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
}
