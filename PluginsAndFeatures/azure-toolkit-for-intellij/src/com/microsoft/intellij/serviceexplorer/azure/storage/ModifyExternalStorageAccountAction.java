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
package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.forms.ExternalStorageAccountForm;
import com.microsoft.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;

@Name("Modify External Storage")
public class ModifyExternalStorageAccountAction extends NodeActionListener {
    private final ExternalStorageNode storageNode;

    public ModifyExternalStorageAccountAction(ExternalStorageNode storageNode) {
        this.storageNode = storageNode;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ExternalStorageAccountForm form = new ExternalStorageAccountForm((Project) storageNode.getProject());
        form.setTitle("Modify External Storage Account");

        for (ClientStorageAccount account : ExternalStorageHelper.getList(storageNode.getProject())) {
            if (account.getName().equals(storageNode.getClientStorageAccount().getName())) {
                form.setStorageAccount(account);
            }
        }


        form.setOnFinish(new Runnable() {
            @Override
            public void run() {
                ClientStorageAccount oldStorageAccount = storageNode.getClientStorageAccount();
                ClientStorageAccount storageAccount = StorageClientSDKManager.getManager().getStorageAccount(
                        form.getStorageAccount().getConnectionString());
                ClientStorageAccount fullStorageAccount = form.getFullStorageAccount();

                StorageModule parent = (StorageModule) storageNode.getParent();
                parent.removeDirectChildNode(storageNode);
                parent.addChildNode(new ExternalStorageNode(parent, fullStorageAccount));

                ExternalStorageHelper.detach(oldStorageAccount);
                ExternalStorageHelper.add(form.getStorageAccount());
            }
        });

        form.show();
    }
}