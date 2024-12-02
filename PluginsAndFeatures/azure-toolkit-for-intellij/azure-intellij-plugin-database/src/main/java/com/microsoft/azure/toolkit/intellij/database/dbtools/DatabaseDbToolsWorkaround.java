/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.intellij.dbtools.DbToolsWorkarounds;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

public class DatabaseDbToolsWorkaround implements ProjectActivity, DumbAware {

    @Nullable
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                loadMySqlAzureTemplates();
                loadPostgreSqlAzureTemplates();
                loadSqlServerAzureTemplates();
                loadAzureSqlDatabaseAzureTemplates();
            } catch (final Throwable t) {
                // swallow exception for preloading workarounds
                AzureTelemeter.log(AzureTelemetry.Type.ERROR, new HashMap<>(), t);
            }
        });
        return null;
    }

    private static void loadMySqlAzureTemplates() {
        DbToolsWorkarounds.loadDriverTemplate(
                "mysql.8",
                DatabaseServerTypeUIFactory.MYSQL,
                "Azure",
                "jdbc:mysql://{host::localhost}?[:{port::3306}][/{database}?][/{account:az_mysql_server}?][\\?<&,user={user},password={password},{:identifier}={:param}>]");

    }

    private static void loadPostgreSqlAzureTemplates() {
        DbToolsWorkarounds.loadDriverTemplate(
                "postgresql",
                DatabaseServerTypeUIFactory.POSTGRE,
                "Azure",
                "jdbc:postgresql://[{host::localhost}[:{port::5432}]][/{database:database/[^?]+:postgres}?][/{account:az_postgre_server}?][\\?<&,user={user:param},password={password:param},{:identifier}={:param}>]");
    }

    private static void loadSqlServerAzureTemplates() {
        DbToolsWorkarounds.loadDriverTemplate(
                "sqlserver.ms",
                DatabaseServerTypeUIFactory.SQLSERVER,
                "Azure",
                "jdbc:sqlserver://{host:ssrp_host:localhost}[\\\\{instance:ssrp_instance}][:{port:ssrp_port}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?");
    }

    private static void loadAzureSqlDatabaseAzureTemplates() {
        DbToolsWorkarounds.loadDriverTemplate(
                "azure.ms",
                DatabaseServerTypeUIFactory.SQLSERVER,
                "Azure",
                "jdbc:sqlserver://{host:host_ipv6:server.database.windows.net}[:{port::1433}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?");
    }
}
