/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.IntellijDatasourceService;
import com.microsoft.azure.toolkit.lib.common.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.util.AzureLoginHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLNode;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

@Name(OpenMySQLByToolsAction.ACTION_NAME)
public class OpenMySQLByToolsAction extends NodeActionListener {

    public static final String ACTION_NAME = "Open by Database Tools";
    private static final String MYSQL_PATTERN_NAME = "Azure Database for MySQL - %s";
    private static final String MYSQL_DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private final MySQLNode node;
    private final Project project;

    public OpenMySQLByToolsAction(MySQLNode node) {
        super();
        this.node = node;
        this.project = (Project) node.getProject();
    }

    @Override
    public AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.MySQL.CONNECT_TO_SERVER;
    }

    @Override
    public void actionPerformed(NodeActionEvent e) {
        AzureSignInAction.requireSignedIn(project, () -> doActionPerformed(true, project));
    }

    @Override
    protected String getServiceName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.MySQL.CONNECT_TO_SERVER).getServiceName();
    }

    @Override
    protected String getOperationName(NodeActionEvent event) {
        return ActionConstants.parse(ActionConstants.MySQL.CONNECT_TO_SERVER).getOperationName();
    }

    @AzureOperation(name = "mysql.connect_server", params = {"this.node.getServer().name()"}, type = AzureOperation.Type.ACTION)
    private void doActionPerformed(boolean isLoggedIn, Project project) {
        try {
            if (!isLoggedIn ||
                !AzureLoginHelper.isAzureSubsAvailableOrReportError(message("common.error.signIn"))) {
                return;
            }
        } catch (final Exception ex) {
            AzurePlugin.log(message("common.error.signIn"), ex);
            DefaultLoader.getUIHelper().showException(message("common.error.signIn"), ex, message("common.error.signIn"), false, true);
        }
        final IntellijDatasourceService.DatasourceProperties properties = IntellijDatasourceService.DatasourceProperties.builder()
                .name(String.format(MYSQL_PATTERN_NAME, node.getServer().name()))
                .driverClassName(MYSQL_DEFAULT_DRIVER)
                .url(JdbcUrl.mysql(node.getServer().entity().getFullyQualifiedDomainName()).toString())
                .username(node.getServer().entity().getAdministratorLoginName() + "@" + node.getServer().name())
                .build();
        IntellijDatasourceService.getInstance().openDataSourceManagerDialog(project, properties);
    }

}
