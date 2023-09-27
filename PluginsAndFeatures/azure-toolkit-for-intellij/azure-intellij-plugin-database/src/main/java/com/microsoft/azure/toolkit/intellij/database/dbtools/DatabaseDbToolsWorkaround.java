/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.dbtools;

import com.intellij.database.dataSource.DatabaseDriverImpl;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.url.template.UrlTemplate;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DatabaseDbToolsWorkaround implements StartupActivity, DumbAware {

    @Nullable
    @Override
    public void runActivity(@Nonnull Project project) {
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
    }

    private static void loadMySqlAzureTemplates() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl driver = ((DatabaseDriverImpl) manager.getDriver("mysql.8"));
        if (Objects.nonNull(driver)) {
            final List<UrlTemplate> templates = new LinkedList<>(driver.getUrlTemplates());
            templates.removeIf(t -> t.getTemplate().contains(DatabaseServerTypeFactory.MYSQL));
            templates.add(0, new UrlTemplate("Azure", "jdbc:mysql://{host::localhost}?[:{port::3306}][/{database}?][/{account:az_mysql_server}?][\\?<&,user={user},password={password},{:identifier}={:param}>]"));
            driver.setURLTemplates(templates);
        }
    }

    private static void loadPostgreSqlAzureTemplates() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl driver = ((DatabaseDriverImpl) manager.getDriver("postgresql"));
        if (Objects.nonNull(driver)) {
            final List<UrlTemplate> templates = new LinkedList<>(driver.getUrlTemplates());
            templates.removeIf(t -> t.getTemplate().contains(DatabaseServerTypeFactory.POSTGRE));
            templates.add(0, new UrlTemplate("Azure", "jdbc:postgresql://[{host::localhost}[:{port::5432}]][/{database:database/[^?]+:postgres}?][/{account:az_postgre_server}?][\\?<&,user={user:param},password={password:param},{:identifier}={:param}>]"));
            driver.setURLTemplates(templates);
        }
    }

    private static void loadSqlServerAzureTemplates() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl driver = ((DatabaseDriverImpl) manager.getDriver("sqlserver.ms"));
        if (Objects.nonNull(driver)) {
            final List<UrlTemplate> templates = new LinkedList<>(driver.getUrlTemplates());
            templates.removeIf(t -> t.getTemplate().contains(DatabaseServerTypeFactory.SQLSERVER));
            templates.add(0, new UrlTemplate("Azure", "jdbc:sqlserver://{host:ssrp_host:localhost}[\\\\{instance:ssrp_instance}][:{port:ssrp_port}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?"));
            driver.setURLTemplates(templates);
        }
    }

    private static void loadAzureSqlDatabaseAzureTemplates() {
        final DatabaseDriverManager manager = DatabaseDriverManager.getInstance();
        final DatabaseDriverImpl driver = ((DatabaseDriverImpl) manager.getDriver("azure.ms"));
        if (Objects.nonNull(driver)) {
            final List<UrlTemplate> templates = new LinkedList<>(driver.getUrlTemplates());
            templates.removeIf(t -> t.getTemplate().contains(DatabaseServerTypeFactory.SQLSERVER));
            templates.add(0, new UrlTemplate("Azure", "jdbc:sqlserver://{host:host_ipv6:server.database.windows.net}[:{port::1433}][/{account:az_sqlserver_server}?][;<;,user[Name]={user:param},password={password:param},database[Name]={database},{:identifier}={:param}>];?"));
            driver.setURLTemplates(templates);
        }
    }
}
