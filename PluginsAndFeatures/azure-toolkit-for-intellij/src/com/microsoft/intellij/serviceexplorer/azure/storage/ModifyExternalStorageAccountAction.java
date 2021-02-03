/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
