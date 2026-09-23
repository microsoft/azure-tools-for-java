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
import com.microsoft.azure.toolkit.intellij.cosmos.actions.CreateNewDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.actions.OpenCosmosDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.actions.UploadCosmosDocumentAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosContainerAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDatabaseAction;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

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

        final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            final BiConsumer<CosmosDBAccount, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), c);
            final boolean cassandraOn = Registry.is("azure.toolkit.cosmos_cassandra.dbtools.enabled");
            am.registerHandler(CosmosActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> r instanceof MongoCosmosDBAccount || (r instanceof CassandraCosmosDBAccount && cassandraOn), openDatabaseHandler);
        }
    }

    private void openDatabaseTool(Project project, CosmosDBAccount account) {
        final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
        final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not installed.";
        final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";
        final Action<Object> tryUltimate = AzureActionManager.getInstance().getAction(IntellijActionsContributor.TRY_ULTIMATE).bind(account);
        throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, tryUltimate);
    }
}
