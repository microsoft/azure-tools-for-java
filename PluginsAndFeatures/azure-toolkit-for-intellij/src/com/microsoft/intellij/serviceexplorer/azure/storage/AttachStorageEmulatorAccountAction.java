/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
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
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ExternalStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.StorageModule;

import javax.swing.*;

@Name("Attach storage emulator...")
public class AttachStorageEmulatorAccountAction extends NodeActionListener {
    private final String connectionName = "devstoreaccount1";
    private final StorageModule storageModule;

    public AttachStorageEmulatorAccountAction(StorageModule storageModule) {
        this.storageModule = storageModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        for (ClientStorageAccount clientStorageAccount : ExternalStorageHelper.getList(storageModule.getProject())) {
            if (clientStorageAccount.getName().equals(connectionName)) {
                return;
            }
        }

        // https://docs.microsoft.com/en-us/azure/storage/common/storage-use-emulator
        ClientStorageAccount storageAccount = new ClientStorageAccount(connectionName);
        storageAccount.setUseCustomEndpoints(true);
        storageAccount.setProtocol("http");
        storageAccount.setPrimaryKey("Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==");
        storageAccount.setBlobsUri("http://127.0.0.1:10000/devstoreaccount1");
        storageAccount.setTablesUri("http://127.0.0.1:10002/devstoreaccount1");
        storageAccount.setQueuesUri("http://127.0.0.1:10001/devstoreaccount1");

        ExternalStorageNode node = new ExternalStorageNode(storageModule, storageAccount);
        storageModule.addChildNode(node);
        ExternalStorageHelper.add(storageAccount);
    }
}
