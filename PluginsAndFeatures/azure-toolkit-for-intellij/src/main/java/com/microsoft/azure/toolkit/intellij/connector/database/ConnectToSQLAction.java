/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServer;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerNode;

@Name(ConnectToSQLAction.ACTION_NAME)
public class ConnectToSQLAction extends NodeActionListener {

    public static final String ACTION_NAME = "Connect to Project(Preview)";

    private final SqlServerNode node;
    private final Project project;

    public ConnectToSQLAction(SqlServerNode node) {
        super();
        this.node = node;
        this.project = (Project) node.getProject();
    }

    @Override
    public AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.SqlServer.BIND_INTO;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        final ConnectorDialog dialog = new ConnectorDialog(project);
        final SqlServer server = this.node.getServer();
        final Database database = new Database(server.id(), null);
        final DatabaseResource resource = new DatabaseResource(database, DatabaseResource.Definition.SQL_SERVER);
        dialog.setResource(resource);
        dialog.show();
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.SqlServer.LINK_TO_MODULE).getServiceName();
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.SqlServer.LINK_TO_MODULE).getOperationName();
    }
}
