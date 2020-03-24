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
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;

public class ConfirmDialogAction extends NodeActionListener {
    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ExternalStorageNode node = (ExternalStorageNode) e.getAction().getNode();

        final ExternalStorageAccountForm form = new ExternalStorageAccountForm((Project) node.getProject());
        form.setTitle("Storage Account Key Required");
        form.setStorageAccount(node.getClientStorageAccount());

        form.setOnFinish(new Runnable() {
            @Override
            public void run() {
                node.getClientStorageAccount().setPrimaryKey(form.getPrimaryKey());
                ClientStorageAccount clientStorageAccount = StorageClientSDKManager.getManager().getStorageAccount(node.getClientStorageAccount().getConnectionString());

                node.getClientStorageAccount().setPrimaryKey(clientStorageAccount.getPrimaryKey());
                node.getClientStorageAccount().setBlobsUri(clientStorageAccount.getBlobsUri());
                node.getClientStorageAccount().setQueuesUri(clientStorageAccount.getQueuesUri());
                node.getClientStorageAccount().setTablesUri(clientStorageAccount.getTablesUri());

                node.load(false);
            }
        });

        form.show();
    }
}
