/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResourceEntity;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<IAzureResourceEntity> {

    private IDatabaseServer server;

    public void setServer(IDatabaseServer server) {
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
    @Nonnull
    protected List<? extends IAzureResourceEntity> loadItems() throws Exception {
        if (Objects.isNull(server) || !StringUtils.equalsIgnoreCase("READY", server.entity().getState())) {
            return new ArrayList<>();
        }
        return server.databases();
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
