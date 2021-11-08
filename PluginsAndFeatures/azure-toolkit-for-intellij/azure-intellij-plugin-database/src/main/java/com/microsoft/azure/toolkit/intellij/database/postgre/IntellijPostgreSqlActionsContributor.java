/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.database.postgre.PostgreSqlActionsContributor;
import com.microsoft.azure.toolkit.intellij.database.IntellijDatasourceService;
import com.microsoft.azure.toolkit.intellij.database.postgre.creation.CreatePostgreSqlAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerEntity;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijPostgreSqlActionsContributor implements IActionsContributor {
    private static final String NAME_PREFIX = "PostgreSQL - %s";
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzurePostgreSql;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreatePostgreSqlAction.create((e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        //TODO(andxu): add service link
        am.<IAzureResource<?>, AnActionEvent>registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof PostgreSqlServer,
                (r, e) -> AzureTaskManager.getInstance().runLater(() -> {
                    AzureMessager.getMessager().info("Connect to PostgreSQL is not supported yet.", "Function not supported");
                }));

        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), (PostgreSqlServer) c);
        am.registerHandler(PostgreSqlActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> true, openDatabaseHandler);

    }

    @AzureOperation(name = "postgre|server.open_by_database_tools", params = {"server.entity().getName()"}, type = AzureOperation.Type.ACTION)
    private void openDatabaseTool(Project project, PostgreSqlServer server) {
        PostgreSqlServerEntity entity = server.entity();
        IntellijDatasourceService.DatasourceProperties properties = IntellijDatasourceService.DatasourceProperties.builder()
                .name(String.format(NAME_PREFIX, entity.getName()))
                .driverClassName(DEFAULT_DRIVER_CLASS_NAME)
                .url(JdbcUrl.postgre(entity.getFullyQualifiedDomainName(), "postgres").toString())
                .username(entity.getAdministratorLoginName() + "@" + entity.getName())
                .build();
        AzureTaskManager.getInstance().runLater(() -> {
            IntellijDatasourceService.getInstance().openDataSourceManagerDialog(project, properties);
        });

    }
}
