/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import lombok.Getter;

import java.util.Objects;

public class DatabaseComboBox<T extends IAzureResourceEntity, S extends IDatabaseServer> extends AzureComboBox<T> {

    @Getter
    private S server;

    public void setServer(S server) {
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
        return Objects.nonNull(item) && item instanceof IAzureResourceEntity ? ((IAzureResourceEntity) item).getName() : super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
