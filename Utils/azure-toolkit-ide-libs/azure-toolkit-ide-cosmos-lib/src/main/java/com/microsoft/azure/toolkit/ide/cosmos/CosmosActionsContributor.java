/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cosmos;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class CosmosActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.cosmos.service";
    public static final String ACCOUNT_ACTIONS = "actions.cosmos.account";
    public static final String SQL_ACCOUNT_ACTIONS = "actions.cosmos.sql_account";
    public static final String MONGO_ACCOUNT_ACTIONS = "actions.cosmos.mongo_account";
    public static final String CASSANDRA_ACCOUNT_ACTIONS = "actions.cosmos.cassandra_account";
    public static final String SQL_DATABASE_ACTIONS = "actions.cosmos.sql_database";
    public static final String MONGO_DATABASE_ACTIONS = "actions.cosmos.mongo_database";
    public static final String CASSANDRA_KEYSPACE_ACTIONS = "actions.cosmos.cassandra_keyspace";
    public static final String SQL_CONTAINER_ACTIONS = "actions.cosmos.sql_container";
    public static final String MONGO_COLLECTION_ACTIONS = "actions.cosmos.mongo_collection";
    public static final String CASSANDRA_TABLE_ACTIONS = "actions.cosmos.cassandra_table";

    public static final Action.Id<CosmosDBAccount> OPEN_DATABASE_TOOL = Action.Id.of("cosmos.open_database_tool");
    public static final Action.Id<CosmosDBAccount> OPEN_DATA_EXPLORER = Action.Id.of("cosmos.open_data_explorer.account");
    public static final Action.Id<CosmosDBAccount> COPY_CONNECTION_STRING = Action.Id.of("cosmos.copy_connection_string.account");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_KUBERNETES_SERVICE = Action.Id.of("group.create_cosmos_db_account");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder openDatabaseTool = new ActionView.Builder("Open by Database Tools", AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("cosmos.open_database_tool.account", ((AzResource<?, ?, ?>) r).getName())).orElse(null))
            .enabled(s -> s instanceof CosmosDBAccount && ((AzResourceBase) s).getFormalStatus().isRunning());
        final Action<CosmosDBAccount> action = new Action<>(OPEN_DATABASE_TOOL, openDatabaseTool);
        action.setShortcuts("control alt D");
        am.registerAction(OPEN_DATABASE_TOOL, action);

        final Consumer<CosmosDBAccount> copyConnectionString = resource -> {
            final String connectionString = resource.listConnectionStrings().getPrimaryConnectionString();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(connectionString), null);
            AzureMessager.getMessager().info("Connection string copied");
        };
        final ActionView.Builder copyConnectionStringView = new ActionView.Builder("Copy Connection String")
                .title(s -> Optional.ofNullable(s).map(r -> description("cosmos.copy_connection_string.account", ((CosmosDBAccount) r).getName())).orElse(null))
                .enabled(s -> s instanceof CosmosDBAccount && ((CosmosDBAccount) s).getFormalStatus().isConnected());
        final Action<CosmosDBAccount> copyConnectionStringAction = new Action<>(COPY_CONNECTION_STRING, copyConnectionString, copyConnectionStringView);
        am.registerAction(COPY_CONNECTION_STRING, copyConnectionStringAction);

        final Consumer<CosmosDBAccount> openAzureStorageExplorer = resource ->
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(resource.getPortalUrl() + "/dataExplorer");
        final ActionView.Builder openAzureStorageExplorerView = new ActionView.Builder("Open Data Explorer")
                .title(s -> Optional.ofNullable(s).map(r -> description("cosmos.open_data_explorer.account", ((CosmosDBAccount) r).getName())).orElse(null))
                .enabled(s -> s instanceof CosmosDBAccount && ((CosmosDBAccount) s).getFormalStatus().isConnected());
        final Action<CosmosDBAccount> openDataExplorerAction = new Action<>(OPEN_DATA_EXPLORER, openAzureStorageExplorer, openAzureStorageExplorerView);
        am.registerAction(OPEN_DATA_EXPLORER, openDataExplorerAction);

        final ActionView.Builder createClusterView = new ActionView.Builder("Azure Cosmos DB")
                .title(s -> Optional.ofNullable(s).map(r ->
                        description("group.create_cosmos_db_account.group", ((ResourceGroup) r).getName())).orElse(null))
                .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_KUBERNETES_SERVICE, new Action<>(GROUP_CREATE_KUBERNETES_SERVICE, createClusterView));

    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                CosmosActionsContributor.OPEN_DATA_EXPLORER,
                "---",
                CosmosActionsContributor.OPEN_DATABASE_TOOL,
                "---",
                ResourceCommonActionsContributor.CREATE,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                CosmosActionsContributor.COPY_CONNECTION_STRING
        );
        am.registerGroup(ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(SQL_ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(MONGO_ACCOUNT_ACTIONS, accountActionGroup);
        am.registerGroup(CASSANDRA_ACCOUNT_ACTIONS, accountActionGroup);

        final ActionGroup databaseGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                "---",
                ResourceCommonActionsContributor.CREATE,
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SQL_DATABASE_ACTIONS, databaseGroup);
        am.registerGroup(MONGO_DATABASE_ACTIONS, databaseGroup);
        am.registerGroup(CASSANDRA_KEYSPACE_ACTIONS, databaseGroup);

        final ActionGroup collectionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SQL_CONTAINER_ACTIONS, collectionGroup);
        am.registerGroup(MONGO_COLLECTION_ACTIONS, collectionGroup);
        am.registerGroup(CASSANDRA_TABLE_ACTIONS, collectionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
