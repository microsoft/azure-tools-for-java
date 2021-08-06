/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.function.Function;

public class DatabaseComboBox<S, T> extends AzureComboBox<T> {

    @Getter
    private S server;
    @Setter
    private Function<T, String> itemTextFunc;

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
        return Objects.nonNull(itemTextFunc) && Objects.nonNull(item) ? itemTextFunc.apply((T) item) : super.getItemText(item);
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
