package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CassandraCosmosDBAccountResourceDefinition extends AzureServiceResource.Definition<CassandraKeyspace> implements SpringSupported<CassandraKeyspace> {
    public static final CassandraCosmosDBAccountResourceDefinition INSTANCE = new CassandraCosmosDBAccountResourceDefinition();

    public CassandraCosmosDBAccountResourceDefinition() {
        super("Azure.Cosmos.Cassandra", "Azure Cosmos DB account (Cassandra)", AzureIcons.Cosmos.MODULE.getIconPath());
    }

    @Override
    public CassandraKeyspace getResource(String dataId) {
        return Azure.az(AzureCosmosService.class).getById(dataId);
    }

    @Override
    public List<Resource<CassandraKeyspace>> getResources(Project project) {
        return Azure.az(AzureCosmosService.class).list().stream()
            .flatMap(m -> m.getCosmosDBAccountModule().list().stream())
            .filter(a -> a instanceof CassandraCosmosDBAccount)
            .flatMap(a -> ((CassandraCosmosDBAccount) a).keySpaces().list().stream())
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
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.data.cassandra.contact-points", String.format("${%s_CONTACT_POINT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.cassandra.port", String.format("${%s_PORT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.cassandra.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.cassandra.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.cassandra.keyspace-name", String.format("${%s_KEYSPACE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.cassandra.schema-action", "create_if_not_exists"));
        properties.add(Pair.of("spring.data.cassandra.ssl", "true"));
        return properties;
    }
}
