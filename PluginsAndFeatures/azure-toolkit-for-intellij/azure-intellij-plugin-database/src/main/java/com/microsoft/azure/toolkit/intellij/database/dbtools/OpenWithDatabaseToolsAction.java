/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.Dbms;
import com.intellij.database.autoconfig.DataSourceDetector;
import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.view.ui.DataSourceManagerDialog;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.dbtools.DatabasePlugin;
import com.microsoft.azure.toolkit.intellij.dbtools.DatabaseTools;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.mysql.MySqlServer;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.MicrosoftSqlServer;

import javax.annotation.Nonnull;

public class OpenWithDatabaseToolsAction {
    public static void openDatabaseTool(Project project, @Nonnull IDatabaseServer<?> server) {
        DatabaseTools.openDatabaseTool(project, server, OpenWithDatabaseToolsAction::openDataSourceManagerDialog);
    }

    @AzureOperation(name = "boundary/$database.open_database_tools.server", params = {"server.getName()"}, source = "server")
    private static void openDataSourceManagerDialog(IDatabaseServer<?> server, Project project) {
        final DataSourceRegistry registry = new DataSourceRegistry(project);
        final DbPsiFacade dbPsiFacade = DbPsiFacade.getInstance(project);
        final DataSourceDetector.Builder builder = registry.getBuilder()
            .withDriverClass(server.getJdbcUrl().getDefaultDriverClass())
            .withUrl(server.getJdbcUrl().toString())
            .withJdbcAdditionalProperty(DatabaseServerParamEditor.KEY_DB_SERVER_ID, server.getId())
            .withUser(server.getFullAdminName())
            .withDbms(getDbms(server))
            .commit();
        DataSourceManagerDialog.showDialog(dbPsiFacade, registry);
    }

    private static Dbms getDbms(IDatabaseServer<?> server) {
        if (server instanceof MicrosoftSqlServer) {
            return Dbms.MSSQL;
        } else if (server instanceof MySqlServer) {
            return Dbms.MYSQL;
        } else if (server instanceof PostgreSqlServer) {
            return Dbms.POSTGRES;
        }
        return Dbms.UNKNOWN;
    }
}
