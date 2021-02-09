/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage.asm;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ContainerNode;

import java.util.List;

public class ClientBlobModule extends RefreshableNode {
    private static final String BLOBS = "Blobs";
    final ClientStorageAccount storageAccount;

    public ClientBlobModule(ClientStorageNode parent, ClientStorageAccount storageAccount) {
        super(BLOBS + storageAccount.getName(), BLOBS, parent, null);
        this.parent = parent;
        this.storageAccount = storageAccount;
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        final List<BlobContainer> blobContainers = StorageClientSDKManager.getManager().getBlobContainers(storageAccount.getConnectionString());

        for (BlobContainer blobContainer : blobContainers) {
            addChildNode(new ContainerNode(this, storageAccount, blobContainer));
        }
    }

    public ClientStorageAccount getStorageAccount() {
        return storageAccount;
    }
}
