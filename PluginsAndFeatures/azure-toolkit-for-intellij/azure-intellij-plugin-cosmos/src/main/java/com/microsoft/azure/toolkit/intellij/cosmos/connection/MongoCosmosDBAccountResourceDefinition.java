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
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MongoCosmosDBAccountResourceDefinition extends AzureServiceResource.Definition<MongoDatabase> implements SpringSupported<MongoDatabase> {
    public static final MongoCosmosDBAccountResourceDefinition INSTANCE = new MongoCosmosDBAccountResourceDefinition();

    public MongoCosmosDBAccountResourceDefinition() {
        super("Azure.Cosmos.Mongo", "Azure Cosmos DB account (Mongo)", AzureIcons.Cosmos.MODULE.getIconPath());
    }

    @Override
    public MongoDatabase getResource(String dataId) {
        return Azure.az(AzureCosmosService.class).getById(dataId);
    }

    @Override
    public List<Resource<MongoDatabase>> getResources(Project project) {
        return Azure.az(AzureCosmosService.class).list().stream()
            .flatMap(m -> m.getCosmosDBAccountModule().list().stream())
            .filter(a -> a instanceof MongoCosmosDBAccount)
            .flatMap(a -> ((MongoCosmosDBAccount) a).mongoDatabases().list().stream())
            .map(this::define).toList();
    }

    @Override
    public AzureFormJPanel<Resource<MongoDatabase>> getResourcePanel(Project project) {
        final Function<Subscription, ? extends List<MongoCosmosDBAccount>> accountLoader = subscription ->
            Azure.az(AzureCosmosService.class).databaseAccounts(subscription.getId()).list().stream()
                .filter(account -> account instanceof MongoCosmosDBAccount)
                .map(account -> (MongoCosmosDBAccount) account).collect(Collectors.toList());
        final Function<MongoCosmosDBAccount, ? extends List<? extends MongoDatabase>> databaseLoader = account -> account.mongoDatabases().list();
        return new CosmosDatabaseResourcePanel<>(this, accountLoader, databaseLoader);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<MongoDatabase> data, Project project) {
        final MongoDatabase database = data.getData();
        final CosmosDBAccount account = database.getParent();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_DATABASE", Connection.ENV_PREFIX), database.getName());
        env.put(String.format("%s_CONNECTION_STRING", Connection.ENV_PREFIX), account.listConnectionStrings().getPrimaryConnectionString());
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.data.mongodb.database", String.format("${%s_DATABASE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.mongodb.uri", String.format("${%s_CONNECTION_STRING}", Connection.ENV_PREFIX)));
        return properties;
    }
}
