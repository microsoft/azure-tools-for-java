package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.database.mysql.creation.CreateMySqlAction;
import com.microsoft.azure.toolkit.intellij.database.postgre.creation.CreatePostgreSqlAction;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.creation.CreateSqlServerAction;
import com.microsoft.azure.toolkit.intellij.dbtools.NoResourceTipLabel;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.awt.event.InputEvent;

public class NoServersTipLabel extends NoResourceTipLabel {
    private static final String NO_SERVERS_TIPS = "<html>No existing %s servers in Azure. You can <a href=''>create one</a> first.</html>";
    @Nonnull
    private final DatabaseServerClass databaseServerClass;

    public NoServersTipLabel(@Nonnull DatabaseServerClass databaseServerClass) {
        super(String.format(NO_SERVERS_TIPS, databaseServerClass.getServerName()));
        this.databaseServerClass = databaseServerClass;
    }

    @Override
    @AzureOperation(name = "user/$database.create_server_from_dbtools")
    protected void createResourceInIde(InputEvent e) {
        OperationContext.current().setTelemetryProperty("serviceName", databaseServerClass.getServiceName());
        super.createResourceInIde(e);
    }

    @Nullable
    @Override
    protected Class<? extends AzService> getClazzForNavigationToExplorer() {
        return switch (databaseServerClass) {
            case MySql -> AzureMySql.class;
            case MsSql -> AzureSqlServer.class;
            case Postgres -> AzurePostgreSql.class;
            default -> null;
        };
    }

    @Override
    protected void createResource(Project project) {
        switch (databaseServerClass) {
            case MySql -> CreateMySqlAction.create(project, null);
            case MsSql -> CreateSqlServerAction.create(project, null);
            case Postgres -> CreatePostgreSqlAction.create(project, null);
        };
    }
}
