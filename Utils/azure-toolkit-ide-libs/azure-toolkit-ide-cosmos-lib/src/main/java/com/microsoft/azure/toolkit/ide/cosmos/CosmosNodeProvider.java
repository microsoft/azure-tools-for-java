/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.cosmos;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraTable;
import com.microsoft.azure.toolkit.lib.cosmos.model.DatabaseAccountKind;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCollection;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlContainer;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;

public class CosmosNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Azure Cosmos DB";
    private static final String ICON = AzureIcons.Cosmos.MODULE.getIconPath();

    @Nullable
    @Override
    public AzureCosmosService getRoot() {
        return Azure.az(AzureCosmosService.class);
    }

    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureCosmosService || data instanceof CosmosDBAccount || data instanceof MongoDatabase || data instanceof MongoCollection ||
                data instanceof CassandraKeyspace || data instanceof CassandraTable || data instanceof SqlDatabase || data instanceof SqlContainer;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureCosmosService) {
            final AzureCosmosService service = ((AzureCosmosService) data);
            final Function<AzureCosmosService, List<CosmosDBAccount>> listFunction = acs -> acs.list().stream().flatMap(m -> m.databaseAccounts().list().stream())
                    .collect(Collectors.toList());
            return new Node<>(service).view(new AzureCosmosServiceLabelView(service, NAME, ICON))
                    .actions(CosmosActionsContributor.SERVICE_ACTIONS)
                    .addChildren(listFunction, (account, serviceNode) -> this.createNode(account, serviceNode, manager));
        } else if (data instanceof SqlCosmosDBAccount) {
            final SqlCosmosDBAccount sqlCosmosDBAccount = (SqlCosmosDBAccount) data;
            return new Node<>(sqlCosmosDBAccount)
                    .view(new CosmosDBAccountLabelView<>(sqlCosmosDBAccount))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.SQL_ACCOUNT_ACTIONS)
                    .addChildren(account -> account.sqlDatabases().list(), (database, accountNode) -> this.createNode(database, accountNode, manager));
        } else if (data instanceof MongoCosmosDBAccount) {
            final MongoCosmosDBAccount mongoCosmosDBAccount = (MongoCosmosDBAccount) data;
            return new Node<>(mongoCosmosDBAccount)
                    .view(new CosmosDBAccountLabelView<>(mongoCosmosDBAccount))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.MONGO_ACCOUNT_ACTIONS)
                    .addChildren(account -> account.mongoDatabases().list(), (database, accountNode) -> this.createNode(database, accountNode, manager));
        } else if (data instanceof CassandraCosmosDBAccount) {
            final CassandraCosmosDBAccount cassandraCosmosDBAccount = (CassandraCosmosDBAccount) data;
            return new Node<>(cassandraCosmosDBAccount)
                    .view(new CosmosDBAccountLabelView<>(cassandraCosmosDBAccount))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.CASSANDRA_ACCOUNT_ACTIONS)
                    .addChildren(account -> account.keySpaces().list(), (keyspace, accountNode) -> this.createNode(keyspace, accountNode, manager));
        } else if (data instanceof CosmosDBAccount) {
            // for other cosmos db account (table/graph...)
            final CosmosDBAccount account = (CosmosDBAccount) data;
            return new Node<>(account)
                    .view(new CosmosDBAccountLabelView<>(account))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.ACCOUNT_ACTIONS);
        } else if (data instanceof MongoDatabase) {
            final MongoDatabase mongoDatabase = (MongoDatabase) data;
            return new Node<>(mongoDatabase)
                    .view(new AzureResourceLabelView<>(mongoDatabase))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.MONGO_DATABASE_ACTIONS)
                    .addChildren(database -> database.collections().list(), (collection, databaseNode) -> this.createNode(collection, databaseNode, manager));
        } else if (data instanceof MongoCollection) {
            final MongoCollection table = (MongoCollection) data;
            return new Node<>(table)
                    .view(new AzureResourceLabelView<>(table))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.MONGO_COLLECTION_ACTIONS);
        } else if (data instanceof SqlDatabase) {
            final SqlDatabase sqlDatabase = (SqlDatabase) data;
            return new Node<>(sqlDatabase)
                    .view(new AzureResourceLabelView<>(sqlDatabase))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.SQL_DATABASE_ACTIONS)
                    .addChildren(database -> database.containers().list(), (container, databaseNode) -> this.createNode(container, databaseNode, manager));
        } else if (data instanceof SqlContainer) {
            final SqlContainer table = (SqlContainer) data;
            return new Node<>(table)
                    .view(new AzureResourceLabelView<>(table))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.SQL_CONTAINER_ACTIONS);
        } else if (data instanceof CassandraKeyspace) {
            final CassandraKeyspace cassandraKeyspace = (CassandraKeyspace) data;
            return new Node<>(cassandraKeyspace)
                    .view(new AzureResourceLabelView<>(cassandraKeyspace))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.CASSANDRA_KEYSPACE_ACTIONS)
                    .addChildren(keyspace -> keyspace.tables().list(), (table, keyspaceNode) -> this.createNode(table, keyspaceNode, manager));
        } else if (data instanceof CassandraTable) {
            final CassandraTable table = (CassandraTable) data;
            return new Node<>(table)
                    .view(new AzureResourceLabelView<>(table))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(CosmosActionsContributor.CASSANDRA_TABLE_ACTIONS);
        }
        return null;
    }

    static class AzureCosmosServiceLabelView extends AzureServiceLabelView<AzureCosmosService> {

        public AzureCosmosServiceLabelView(@NotNull AzureCosmosService service, String label, String iconPath) {
            super(service, label, iconPath);
        }

        @Override
        public void onEvent(AzureEvent event) {
            final Object source = event.getSource();
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            if (source instanceof AzService && source.equals(this.getService())) {
                tm.runLater(this::refreshChildren);
            }
        }
    }

    static class CosmosDBAccountLabelView<T extends CosmosDBAccount> extends AzureResourceLabelView<T> {

        public CosmosDBAccountLabelView(@NotNull T resource) {
            super(resource, account -> account.getFormalStatus().isRunning() ?
                    Optional.ofNullable(account.getKind()).map(DatabaseAccountKind::getValue).orElse("Unknown") : account.getStatus(), DEFAULT_AZURE_RESOURCE_ICON_PROVIDER);
        }
    }
}
