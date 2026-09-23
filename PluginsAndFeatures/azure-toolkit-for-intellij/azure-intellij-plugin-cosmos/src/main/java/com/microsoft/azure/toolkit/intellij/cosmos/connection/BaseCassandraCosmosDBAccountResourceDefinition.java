package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseCassandraCosmosDBAccountResourceDefinition extends AzureServiceResource.Definition<CassandraKeyspace> {

    public BaseCassandraCosmosDBAccountResourceDefinition() {
        super("Azure.Cosmos.Cassandra", "Azure Cosmos DB account (Cassandra)", AzureIcons.Cosmos.MODULE.getIconPath());
    }

    @Override
    public CassandraKeyspace getResource(String dataId, final String id) {
        return Azure.az(AzureCosmosService.class).getById(dataId);
    }

    @Override
    public List<Resource<CassandraKeyspace>> getResources(Project project) {
        return Azure.az(AzureCosmosService.class).list().stream()
            .flatMap(m -> m.getCosmosDBAccountModule().list().stream())
            .filter(a -> a instanceof CassandraCosmosDBAccount)
            .flatMap(s -> {
                try {
                    return ((CassandraCosmosDBAccount) s).keySpaces().list().stream();
                } catch (final Throwable e) {
                    return Stream.empty();
                }
            })
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<CassandraKeyspace>> getResourcePanel(Project project) {
        final Function<Subscription, ? extends List<CassandraCosmosDBAccount>> accountLoader = subscription ->
            Azure.az(AzureCosmosService.class).databaseAccounts(subscription.getId()).list().stream()
                .filter(account -> account instanceof CassandraCosmosDBAccount)
                .map(account -> (CassandraCosmosDBAccount) account).collect(Collectors.toList());
        final Function<CassandraCosmosDBAccount, ? extends List<? extends CassandraKeyspace>> databaseLoader = account -> account.keySpaces().list();
        return new CosmosDatabaseResourcePanel<>(this, accountLoader, databaseLoader);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<CassandraKeyspace> data, Project project) {
        final CassandraKeyspace keyspace = data.getData();
        final CassandraCosmosDBAccount account = (CassandraCosmosDBAccount) keyspace.getParent();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_CONTACT_POINT", Connection.ENV_PREFIX), account.getContactPoint());
        env.put(String.format("%s_PORT", Connection.ENV_PREFIX), String.valueOf(account.getPort()));
        env.put(String.format("%s_USERNAME", Connection.ENV_PREFIX), account.getName());
        env.put(String.format("%s_PASSWORD", Connection.ENV_PREFIX), account.listKeys().getPrimaryMasterKey());
        env.put(String.format("%s_KEYSPACE", Connection.ENV_PREFIX), keyspace.getName());
        env.put(String.format("%s_REGION", Connection.ENV_PREFIX), keyspace.getRegion().getLabel());
        return env;
    }
}
