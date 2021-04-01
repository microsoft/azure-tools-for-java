/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UsernameComboBox extends AzureComboBox<String> {

    private static final String SEPARATOR = "@";

    private Server server;

    public UsernameComboBox() {
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
    protected List<? extends String> loadItems() {
        if (server == null) {
            return new ArrayList<>();
        }
        List<String> usernames = new ArrayList<>();
        usernames.add(server.administratorLogin() + SEPARATOR + server.name());
        return usernames;
    }

    @Override
    public boolean isRequired() {
        return true;
    }
}
