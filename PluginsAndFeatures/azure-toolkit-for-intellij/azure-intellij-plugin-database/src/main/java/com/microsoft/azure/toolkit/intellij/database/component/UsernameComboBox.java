/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import lombok.Getter;

import java.util.Objects;

public class UsernameComboBox extends AzureComboBox<String> {

    @Getter
    private IDatabaseServer<?> server;

    public void setServer(IDatabaseServer<?> server) {
        if (Objects.equals(server, this.server)) {
            return;
        }
        this.server = server;
        if (server == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
