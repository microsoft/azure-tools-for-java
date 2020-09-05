/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2020 JetBrains s.r.o.
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
package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.forms.CreateBlobContainerForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.BlobModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.asm.ClientBlobModule;

@Name("New Blob container")
public class CreateBlobContainer extends NodeActionListener {
    private RefreshableNode parent;

    public CreateBlobContainer(BlobModule parent) {
        this.parent = parent;
    }

    public CreateBlobContainer(ClientBlobModule parent) {
        this.parent = parent;
    }

    public CreateBlobContainer(StorageNode parent) {
        this.parent = parent;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateBlobContainerForm form = new CreateBlobContainerForm((Project) parent.getProject());
        if (parent instanceof BlobModule) {
            form.setConnectionString(StorageClientSDKManager.getConnectionString(((BlobModule) parent).getStorageAccount()));
        } else if (parent instanceof ClientBlobModule) {
            form.setConnectionString(((ClientBlobModule) parent).getStorageAccount().getConnectionString());
        } else if (parent instanceof StorageNode) {
            form.setConnectionString(StorageClientSDKManager.getConnectionString(((StorageNode) parent).getStorageAccount()));
            form.setSubscription(new SubscriptionDetail(((StorageNode)parent).getSubscriptionId(), null, null, true));
        }

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                parent.removeAllChildNodes();
                parent.load(false);
            }
        });

        form.show();
    }

    @Override
    protected @Nullable String getIconPath() {
        return "AddEntity.svg";
    }
}