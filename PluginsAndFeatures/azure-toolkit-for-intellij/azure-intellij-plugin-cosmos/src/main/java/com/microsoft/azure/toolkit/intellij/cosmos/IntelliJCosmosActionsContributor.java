/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos;

import com.intellij.database.autoconfig.DataSourceRegistry;
import com.intellij.database.dataSource.DatabaseDriverManager;
import com.intellij.database.dataSource.LocalDataSource;
import com.intellij.database.dataSource.LocalDataSourceManager;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.intellij.database.psi.DbPsiFacadeImpl;
import com.intellij.database.view.ui.DataSourceManagerDialog;
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
import com.microsoft.azure.toolkit.intellij.cosmos.connection.CassandraCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.MongoCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.connection.SqlCosmosDBAccountResourceDefinition;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDBAccountAction;
import com.microsoft.azure.toolkit.intellij.cosmos.creation.CreateCosmosDatabaseAction;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.ICosmosDatabaseDraft;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.intellij.cosmos.dbtools.AzureCosmosDbAccountParamEditor.KEY_COSMOS_ACCOUNT_ID;
import static com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccountDraft.Config.getDefaultConfig;
import static com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseConfig.getDefaultDatabaseConfig;

public class IntelliJCosmosActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> serviceCondition = (r, e) -> r instanceof AzureCosmosService;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, serviceCondition, handler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateHandler = (r, e) ->
                CreateCosmosDBAccountAction.create(e.getProject(), getDefaultConfig(r));
        am.registerHandler(CosmosActionsContributor.GROUP_CREATE_COSMOS_SERVICE, (r, e) -> true, groupCreateHandler);

        final Function<MongoCosmosDBAccount, MongoDatabase> mongoFunction = account -> account.mongoDatabases().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((MongoCosmosDBAccount) r, mongoFunction, MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof MongoDatabase, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((MongoDatabase) r, MongoCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));

        final Function<SqlCosmosDBAccount, SqlDatabase> sqlFunction = account -> account.sqlDatabases().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((SqlCosmosDBAccount) r, sqlFunction, SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof SqlDatabase, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((SqlDatabase) r, SqlCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));

        final Function<CassandraCosmosDBAccount, CassandraKeyspace> cassandraFunction = account -> account.keySpaces().list().stream().findFirst().orElse(null);
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraCosmosDBAccount, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((CassandraCosmosDBAccount) r, cassandraFunction, CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, (r, e) -> r instanceof CassandraKeyspace, (AzResource<?, ?, ?> r, AnActionEvent e) ->
                openResourceConnector((CassandraKeyspace) r, CassandraCosmosDBAccountResourceDefinition.INSTANCE, e.getProject()));

        final BiFunction<MongoCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> mongoDraftSupplier = (account, config) ->
                (ICosmosDatabaseDraft<?, ?>) account.mongoDatabases().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof MongoCosmosDBAccount, (Object r, AnActionEvent e) ->
                CreateCosmosDatabaseAction.create(e.getProject(), (MongoCosmosDBAccount) r, mongoDraftSupplier, getDefaultDatabaseConfig()));

        final BiFunction<SqlCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> sqlDraftSupplier = (account, config) ->
                (ICosmosDatabaseDraft<?, ?>) account.sqlDatabases().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof SqlCosmosDBAccount, (Object r, AnActionEvent e) ->
                CreateCosmosDatabaseAction.create(e.getProject(), (SqlCosmosDBAccount) r, sqlDraftSupplier, getDefaultDatabaseConfig()));

        final BiFunction<CassandraCosmosDBAccount, DatabaseConfig, ICosmosDatabaseDraft<?, ?>> cassandraDraftSupplier = (account, config) ->
                (ICosmosDatabaseDraft<?, ?>) account.keySpaces().getOrDraft(config.getName(), account.getResourceGroupName());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof CassandraCosmosDBAccount, (Object r, AnActionEvent e) ->
                CreateCosmosDatabaseAction.create(e.getProject(), (CassandraCosmosDBAccount) r, cassandraDraftSupplier, getDefaultDatabaseConfig()));

        final BiConsumer<CosmosDBAccount, AnActionEvent> openDatabaseHandler = (c, e) -> openDatabaseTool(e.getProject(), c);
        final boolean cassandraOn = Registry.is("azure.toolkit.cosmos_cassandra.dbtools.enabled");
        am.registerHandler(CosmosActionsContributor.OPEN_DATABASE_TOOL, (r, e) -> r instanceof MongoCosmosDBAccount || (r instanceof CassandraCosmosDBAccount && cassandraOn), openDatabaseHandler);
    }

    @AzureOperation(name = "cosmos.open_database_tools.account", params = {"account.getName()"}, type = AzureOperation.Type.ACTION)
    private void openDatabaseTool(Project project, CosmosDBAccount account) {
        final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
        final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not installed.";
        final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, IntellijActionsContributor.TRY_ULTIMATE);
        }
        AzureTaskManager.getInstance().runLater(() -> {
            final DataSourceRegistry registry = new DataSourceRegistry(project);
            final String driver = account instanceof MongoCosmosDBAccount ? "az_cosmos_mongo" : "az_cosmos_cassandra";
            final LocalDataSource ds = DatabaseDriverManager.getInstance().getDriver(driver).createDataSource(null);
            final DbPsiFacade facade = DbPsiFacade.getInstance(project);
            final LocalDataSourceManager manager = LocalDataSourceManager.getInstance(project);
            final DbDataSource newElement = ((DbPsiFacadeImpl) facade).createDataSourceWrapperElement(ds, manager);
            ds.setAdditionalProperty(KEY_COSMOS_ACCOUNT_ID, account.getId());
            DataSourceManagerDialog.showDialog(facade, newElement, null, null, null);
        });
    }

    private <T extends AzResource<?, ?, ?>> void openResourceConnector(@Nonnull final T resource, @Nonnull final AzureServiceResource.Definition<T> definition, Project project) {
        final Function<AzResource<?, ?, ?>, AzureString> titleSupplier = r -> OperationBundle.description("resource.connect_resource.resource", r.getName());
        AzureTaskManager.getInstance().runLater(titleSupplier.apply(resource), () -> {
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setResource(new AzureServiceResource<>(resource, definition));
            dialog.show();
        });
    }

    private <T extends AzResource<?, ?, ?>, R extends CosmosDBAccount> void openResourceConnector(@Nonnull final R account, @Nonnull Function<R, T> databaseFunction,
                                                                                                  @Nonnull final AzureServiceResource.Definition<T> definition, Project project) {
        final T database = databaseFunction.apply(account);
        if (Objects.isNull(database)) {
            AzureMessager.getMessager().warning(AzureString.format("Can not connect to %s as there is no database in selected account", account.getName()));
        } else {
            openResourceConnector(database, definition, project);
        }
    }
}
