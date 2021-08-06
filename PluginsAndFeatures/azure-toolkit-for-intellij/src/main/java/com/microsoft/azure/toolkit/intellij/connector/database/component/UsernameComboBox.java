/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import lombok.Getter;

import java.util.Objects;

public class UsernameComboBox<S> extends AzureComboBox<String> {

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
    public boolean isRequired() {
        return true;
    }
}
