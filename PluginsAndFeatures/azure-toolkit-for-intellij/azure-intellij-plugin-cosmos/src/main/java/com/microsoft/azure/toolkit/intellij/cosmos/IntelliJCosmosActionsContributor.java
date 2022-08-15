/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.CosmosActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.CassandraCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.MongoCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction.getDefaultConfig;

public class IntelliJCosmosActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCosmosService;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, serviceCondition, handler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateHandler = (r, e) ->
                CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(r));
        am.registerHandler(CosmosActionsContributor.GROUP_CREATE_KUBERNETES_SERVICE, (r, e) -> true, groupCreateHandler);

        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector(((MongoCosmosDBAccount) r).mongoDatabases().list().get(0), MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector(((SqlCosmosDBAccount) r).sqlDatabases().list().get(0), SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector(((CassandraCosmosDBAccount) r).keySpaces().list().get(0), CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoDatabase, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((MongoDatabase) r, MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlDatabase, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((SqlDatabase) r, SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraKeyspace, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((CassandraKeyspace) r, CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
    }

    private <T extends AzResource<?, ?, ?>> void openResourceConnector(@Nonnull final T resource, @Nonnull final AzureServiceResource.Definition<T> definition, Project project) {
        final Function<AzResource<?, ?, ?>, AzureString> titleSupplier = r -> OperationBundle.description("resource.connect_resource.resource", r.getName());
        AzureTaskManager.getInstance().runLater(titleSupplier.apply(resource), () -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setResource(new AzureServiceResource<>(resource, definition));
            dialog.show();
        });
    }
}
