/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.*;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.*;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Function;

public class IntellijJavaCosmosActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {

        final Function<MongoCosmosDBAccount, MongoDatabase> mongoFunction = account -> account.mongoDatabases().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoCosmosDBAccount && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((MongoCosmosDBAccount) r, mongoFunction, MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoDatabase && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((MongoDatabase) r, MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));

        final Function<SqlCosmosDBAccount, SqlDatabase> sqlFunction = account -> account.sqlDatabases().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlCosmosDBAccount && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((SqlCosmosDBAccount) r, sqlFunction, SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlDatabase && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((SqlDatabase) r, SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));

        final Function<CassandraCosmosDBAccount, CassandraKeyspace> cassandraFunction = account -> account.keySpaces().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraCosmosDBAccount && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((CassandraCosmosDBAccount) r, cassandraFunction, CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraKeyspace && r.getFormalStatus().isConnected(), (AzResource r, AnActionEvent e) ->
            openResourceConnector((CassandraKeyspace) r, CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
    }

    private <T extends AzResource> void openResourceConnector(@Nonnull final T resource, @Nonnull final AzureServiceResource.Definition<T> definition, Project project) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setResource(new AzureServiceResource<>(resource, definition));
            dialog.show();
        });
    }

    private <T extends AzResource, R extends CosmosDBAccount> void openResourceConnector(@Nonnull final R account, @Nonnull Function<R, T> databaseFunction,
                                                                                         @Nonnull final AzureServiceResource.Definition<T> definition, Project project) {
        final T database = databaseFunction.apply(account);
        if (Objects.isNull(database)) {
            AzureMessager.getMessager().warning(AzureString.format("Can not connect to %s as there is no database in selected account", account.getName()));
        } else {
            openResourceConnector(database, definition, project);
        }
    }
}
