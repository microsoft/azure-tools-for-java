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