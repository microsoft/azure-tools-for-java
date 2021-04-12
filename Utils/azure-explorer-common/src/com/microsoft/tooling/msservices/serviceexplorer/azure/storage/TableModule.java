/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.List;

public class TableModule extends RefreshableNode {
    private static final String TABLES = "Tables";
    final StorageAccount storageAccount;

    public TableModule(ClientStorageNode parent, StorageAccount storageAccount) {
        super(TABLES + storageAccount.name(), TABLES, parent, null);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        removeAllChildNodes();

        final List<Table> tables = StorageClientSDKManager.getManager().getTables(storageAccount);

        for (Table table : tables) {
            addChildNode(new TableNode(this, storageAccount, table));
        }
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}
