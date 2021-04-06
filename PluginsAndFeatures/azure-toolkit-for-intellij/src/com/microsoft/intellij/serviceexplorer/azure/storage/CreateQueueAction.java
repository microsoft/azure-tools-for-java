/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.forms.CreateQueueForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.QueueModule;

@Name("New Queue")
public class CreateQueueAction extends NodeActionListener {
    private QueueModule queueModule;

    public CreateQueueAction(QueueModule queueModule) {
        this.queueModule = queueModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateQueueForm form = new CreateQueueForm((Project) queueModule.getProject());
        form.setStorageAccount(queueModule.getStorageAccount());

        form.setOnCreate(() -> {
            queueModule.removeAllChildNodes();
            queueModule.load(false);
        });

        form.show();
    }

    @Override
    public AzureActionEnum getAction() {
        return AzureActionEnum.CREATE;
    }
}
