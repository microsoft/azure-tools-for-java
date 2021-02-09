/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import java.util.List;

public class BlobModule extends RefreshableNode {
    private static final String BLOBS = "Blobs";
    final StorageAccount storageAccount;

    public BlobModule(ClientStorageNode parent, StorageAccount storageAccount) {
        super(BLOBS + storageAccount.name(), BLOBS, parent, null);
        this.parent = parent;
        this.storageAccount = storageAccount;
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        final List<BlobContainer> blobContainers = StorageClientSDKManager.getManager().getBlobContainers(StorageClientSDKManager.getConnectionString(storageAccount));

        for (BlobContainer blobContainer : blobContainers) {
//            addChildNode(new ContainerNode(this, storageAccount, blobContainer)); todo:
        }
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }
}
