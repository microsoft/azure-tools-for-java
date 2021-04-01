/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.List;

public class QueueModule extends RefreshableNode {
    private static final String QUEUES = "Queues";
    final ClientStorageAccount storageAccount;

    public QueueModule(ClientStorageNode parent, ClientStorageAccount storageAccount) {
        super(QUEUES + storageAccount.getName(), QUEUES, parent, null);

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

    public ClientStorageAccount getStorageAccount() {
        return storageAccount;
    }
}
