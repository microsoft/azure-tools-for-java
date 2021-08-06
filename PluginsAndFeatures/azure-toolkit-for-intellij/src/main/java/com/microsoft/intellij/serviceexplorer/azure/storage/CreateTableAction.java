/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure.storage;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.forms.CreateTableForm;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.TableModule;

@Name("Create new table")
public class CreateTableAction extends NodeActionListener {
    private TableModule tableModule;

    public CreateTableAction(TableModule tableModule) {
        this.tableModule = tableModule;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        CreateTableForm form = new CreateTableForm((Project) tableModule.getProject());

//        form.setStorageAccount(tableModule.getStorageAccount());

        form.setOnCreate(new Runnable() {
            @Override
            public void run() {
                tableModule.getParent().removeAllChildNodes();
                ((ClientStorageNode) tableModule.getParent()).load(false);
            }
        });

        form.show();
    }
}
