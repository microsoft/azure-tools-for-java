/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.link.LinkMySQLToModuleDialog;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;

@Name(LinkMySQLAction.ACTION_NAME)
public class LinkMySQLAction extends NodeActionListener {

    public static final String ACTION_NAME = "Connect to Project";

    private final MySQLNode node;
    private final Project project;

    public LinkMySQLAction(MySQLNode node) {
        super();
        this.node = node;
        this.project = (Project) node.getProject();
    }

    @Override
    public AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.MySQL.BIND_INTO;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final LinkMySQLToModuleDialog dialog = new LinkMySQLToModuleDialog(project, node, null);
        dialog.show();
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.MySQL.LINK_TO_MODULE).getServiceName();
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.MySQL.LINK_TO_MODULE).getOperationName();
    }
}
