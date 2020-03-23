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