/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<MySqlDatabaseEntity> {

    private Subscription subscription;
    private MySqlServer server;

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

    public void setServer(MySqlServer server) {
        if (Objects.equals(server, this.server)) {
            return;
        }
        this.server = server;
        if (server == null || !server.exists()) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof MySqlDatabaseEntity) {
            return ((MySqlDatabaseEntity) item).getName();
        }
        return super.getItemText(item);
    }

    @Override
    @AzureOperation(
        name = "mysql|database.list.server",
        params = {"this.server.entity().getName()"},
        type = AzureOperation.Type.SERVICE
    )
    @Nonnull
    protected List<? extends MySqlDatabaseEntity> loadItems() throws Exception {
        if (Objects.isNull(subscription) || Objects.isNull(server)

            || !StringUtils.equalsIgnoreCase("READY", server.entity().getState())) {
            return new ArrayList<>();
        }
        return server.databases();
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
