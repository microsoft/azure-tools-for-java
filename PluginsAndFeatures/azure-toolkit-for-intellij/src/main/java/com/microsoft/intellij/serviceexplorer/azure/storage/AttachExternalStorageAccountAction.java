/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.forms.ExternalStorageAccountForm;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;

import javax.swing.*;

@Name("Attach external storage account...")
public class AttachExternalStorageAccountAction extends NodeActionListener {
    private final StorageModule storageModule;

    public AttachExternalStorageAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ExternalStorageAccountForm form = new ExternalStorageAccountForm((Project) storageModule.getProject());
        form.setTitle("Attach External Storage Account");

        form.setOnFinish(new Runnable() {
            @Override
            public void run() {
                ClientStorageAccount storageAccount = form.getStorageAccount();
                ClientStorageAccount fullStorageAccount = form.getFullStorageAccount();

                for (ClientStorageAccount clientStorageAccount : ExternalStorageHelper.getList(storageModule.getProject())) {
                    String name = storageAccount.getName();
                    if (clientStorageAccount.getName().equals(name)) {
                        DefaultLoader.getUIHelper().showError(
                                form.getContentPane(),
                                "Storage account with name '" + name + "' already exists.",
                                "Azure Explorer");
                        return;
                    }
                }

                ExternalStorageNode node = new ExternalStorageNode(storageModule, fullStorageAccount);
                storageModule.addChildNode(node);
                ExternalStorageHelper.add(storageAccount);
            }
        });

        form.show();
    }
}
