/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
