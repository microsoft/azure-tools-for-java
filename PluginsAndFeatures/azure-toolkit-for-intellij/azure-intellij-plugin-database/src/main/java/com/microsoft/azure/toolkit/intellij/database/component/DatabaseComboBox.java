/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox<T extends IDatabase> extends AzureComboBox<T> {

    @Getter
    private IDatabaseServer<T> server;

    @Override
    public String getLabel() {
        return "Database";
    }

    public void setServer(IDatabaseServer<T> server) {
        if (Objects.equals(server, this.server)) {
            return;
        }
        this.server = server;
        this.clear();
        if (server == null) {
            return;
        }
        this.reloadItems();
    }

    @Nullable
    @Override
    protected T doGetDefaultValue() {
        final T item = this.getItems().get(0);
        //noinspection unchecked
        return (T) CacheManager.getUsageHistory(item.getClass())
            .peek(v -> Objects.isNull(server) || Objects.equals(server, v.getServer()));
    }

    @Override
    protected String getItemText(Object item) {
        return Objects.nonNull(item) && item instanceof IDatabase ? ((IDatabase) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends T> loadItems() {
        if (Objects.isNull(server)) {
            return new ArrayList<>();
        }
        return server.listDatabases();
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
