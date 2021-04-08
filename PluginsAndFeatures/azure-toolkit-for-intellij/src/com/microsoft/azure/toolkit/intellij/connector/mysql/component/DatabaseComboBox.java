/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql.component;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.MySQLManager;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<DatabaseInner> {
    @Getter
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
    protected List<? extends DatabaseInner> loadItems() {
        final AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
        if (Objects.isNull(server) || Objects.isNull(manager)) {
            return new ArrayList<>();
        }
        final String sid = ResourceId.fromString(server.id()).subscriptionId();
        final MySQLManager mySQLManager = manager.getMySQLManager(sid);
        return mySQLManager.databases().inner().listByServer(server.resourceGroupName(), server.name());
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
