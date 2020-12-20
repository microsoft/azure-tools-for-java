/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.ServerState;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.core.mvp.model.mysql.MySQLMvpModel;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DatabaseComboBox extends AzureComboBox<DatabaseInner> {

    @Setter
    private Subscription subscription;
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
