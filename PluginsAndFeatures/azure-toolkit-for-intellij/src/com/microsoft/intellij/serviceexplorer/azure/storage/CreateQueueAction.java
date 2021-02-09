/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.forms.CreateQueueForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.QueueModule;

@Name("Create new queue")
public class CreateQueueAction extends NodeActionListener {
    private QueueModule queueModule;

    public CreateQueueAction(QueueModule queueModule) {
        this.queueModule = queueModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateQueueForm form = new CreateQueueForm((Project) queueModule.getProject());
//        form.setStorageAccount(queueModule.getStorageAccount());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                queueModule.getParent().removeAllChildNodes();
                ((ClientStorageNode) queueModule.getParent()).load(false);
            }
        });

        form.show();
    }
}
