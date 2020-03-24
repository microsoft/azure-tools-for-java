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
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.asm.ClientBlobModule;

public abstract class ClientStorageNode extends RefreshableNode {
    protected final ClientStorageAccount storageAccount;

    public ClientStorageNode(String id, String name, Node parent, String iconPath, ClientStorageAccount sm) {
        super(id, name, parent, iconPath);
        this.storageAccount = sm;
    }

    public ClientStorageNode(String id, String name, Node parent, String iconPath, ClientStorageAccount sm, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
        this.storageAccount = sm;
    }

    public ClientStorageAccount getClientStorageAccount() {
        return storageAccount;
    }

    protected void fillChildren() {
        ClientBlobModule blobsNode = new ClientBlobModule(this, storageAccount);
        blobsNode.load(false);

        addChildNode(blobsNode);

//        QueueModule queueNode = new QueueModule(this, storageAccount);
//        queueNode.load();
//
//        addChildNode(queueNode);
//
//        TableModule tableNode = new TableModule(this, storageAccount);
//        tableNode.load();
//
//        addChildNode(tableNode);
    }
}
