/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
