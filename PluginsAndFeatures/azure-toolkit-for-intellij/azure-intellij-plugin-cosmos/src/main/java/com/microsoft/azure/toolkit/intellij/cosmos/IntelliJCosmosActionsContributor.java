/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.cosmos.CosmosActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.cosmos.actions.CreateNewDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.actions.OpenCosmosDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.actions.UploadCosmosDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.CassandraCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.MongoCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosContainerAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDatabaseAction;
import com.microsoft.azure.toolkit.intellij.dbtools.DatabasePlugin;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabaseDraft;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocument;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDocumentContainer;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTableDraft;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollectionDraft;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainerDraft;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountDraft.Config.getDefaultConfig;
import static com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig.getDefaultDatabaseConfig;

public class IntelliJCosmosActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCosmosService;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, serviceCondition, handler);

        final BiPredicate<ICosmosDocument, AnActionEvent> documentCondition = (r, e) -> r instanceof ICosmosDocument;
        final BiConsumer<ICosmosDocument, AnActionEvent> documentHandler = (c, e) -> OpenCosmosDocumentAction.open(c, e.getProject());
        am.registerHandler(CosmosActionsContributor.OPEN_DOCUMENT, documentCondition, documentHandler);

        final BiPredicate<ICosmosDocumentContainer<?>, AnActionEvent> importCondition = (r, e) ->
            r instanceof ICosmosDocumentContainer && r.getFormalStatus().isConnected();
        final BiConsumer<ICosmosDocumentContainer<?>, AnActionEvent> importHandler = (c, e) -> UploadCosmosDocumentAction.importDocument(c, e.getProject());
        am.registerHandler(CosmosActionsContributor.IMPORT_DOCUMENT, importCondition, importHandler);

        final BiPredicate<ICosmosDocumentContainer<?>, AnActionEvent> createDocumentCondition = (r, e) ->
            r instanceof ICosmosDocumentContainer && r.getFormalStatus().isConnected();
        final BiConsumer<ICosmosDocumentContainer<?>, AnActionEvent> createDocumentHandler = (c, e) ->
            AzureTaskManager.getInstance().runLater(() -> CreateNewDocumentAction.create(c, e.getProject()));
        am.registerHandler(CosmosActionsContributor.CREATE_DOCUMENT, createDocumentCondition, createDocumentHandler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateHandler = (r, e) ->
            CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(r));
        am.registerHandler(CosmosActionsContributor.GROUP_CREATE_COSMOS_SERVICE, (r, e) -> true, groupCreateHandler);

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

        final BiFunction<MongoCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> mongoDraftSupplier = (account, config) ->
            (ICosmosDatabaseDraft<?, ?>) account.mongoDatabases().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof MongoCosmosDBAccount && ((MongoCosmosDBAccount) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosDatabaseAction.create(e.getProject(), (MongoCosmosDBAccount) r, mongoDraftSupplier, getDefaultDatabaseConfig()));

        final BiFunction<SqlCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> sqlDraftSupplier = (account, config) ->
            (ICosmosDatabaseDraft<?, ?>) account.sqlDatabases().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof SqlCosmosDBAccount && ((SqlCosmosDBAccount) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosDatabaseAction.create(e.getProject(), (SqlCosmosDBAccount) r, sqlDraftSupplier, getDefaultDatabaseConfig()));

        final BiFunction<CassandraCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> cassandraDraftSupplier = (account, config) ->
            (ICosmosDatabaseDraft<?, ?>) account.keySpaces().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof CassandraCosmosDBAccount && ((CassandraCosmosDBAccount) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosDatabaseAction.create(e.getProject(), (CassandraCosmosDBAccount) r, cassandraDraftSupplier, getDefaultDatabaseConfig("keyspace")));

        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof SqlDatabase && ((SqlDatabase) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosContainerAction.createSQLContainer(e.getProject(), (SqlDatabase) r, SqlContainerDraft.SqlContainerConfig.getDefaultConfig()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof MongoDatabase && ((MongoDatabase) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosContainerAction.createMongoCollection(e.getProject(), (MongoDatabase) r, MongoCollectionDraft.MongoCollectionConfig.getDefaultConfig()));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof CassandraKeyspace && ((CassandraKeyspace) r).getFormalStatus().isRunning(), (Object r, AnActionEvent e) ->
            CreateCosmosContainerAction.createCassandraTable(e.getProject(), (CassandraKeyspace) r, CassandraTableDraft.CassandraTableConfig.getDefaultConfig()));

        if (!DatabasePlugin.isInstalled()) {
            final BiConsumer<CosmosDBAccount, AnActionEvent> openDatabaseHandler = (c, e) -> DatabasePlugin.throwTryUltimateIfNotInstalled(c);
            final boolean cassandraOn = Registry.is("azure.toolkit.cosmos_cassandra.dbtools.enabled");
            am.registerHandler(CosmosActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> r instanceof MongoCosmosDBAccount || (r instanceof CassandraCosmosDBAccount && cassandraOn), openDatabaseHandler);
        }
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
