/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.List;

public class QueueModule extends RefreshableNode {
    private static final String QUEUES = "Queues";
    final StorageAccount storageAccount;

    public QueueModule(ClientStorageNode parent, StorageAccount storageAccount) {
        super(QUEUES + storageAccount.name(), QUEUES, parent, null);

        this.storageAccount = storageAccount;
        this.parent = parent;
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        final List<Queue> queues = StorageClientSDKManager.getManager().getQueues(storageAccount);

        for (Queue queue : queues) {
            addChildNode(new QueueNode(this, storageAccount, queue));
        }
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}
