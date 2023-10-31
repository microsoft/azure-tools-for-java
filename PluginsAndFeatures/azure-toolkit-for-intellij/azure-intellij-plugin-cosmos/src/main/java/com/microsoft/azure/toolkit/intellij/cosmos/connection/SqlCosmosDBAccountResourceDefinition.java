package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlCosmosDBAccountResourceDefinition extends AzureServiceResource.Definition<SqlDatabase>
    implements SpringSupported<SqlDatabase>, FunctionSupported<SqlDatabase> {
    public static final SqlCosmosDBAccountResourceDefinition INSTANCE = new SqlCosmosDBAccountResourceDefinition();

    public SqlCosmosDBAccountResourceDefinition() {
        super("Azure.Cosmos.Sql", "Azure Cosmos DB account (SQL)", AzureIcons.Cosmos.MODULE.getIconPath());
    }

    @Override
    public SqlDatabase getResource(String dataId) {
        return Azure.az(AzureCosmosService.class).getById(dataId);
    }

    @Override
    public List<Resource<SqlDatabase>> getResources(Project project) {
        return Azure.az(AzureCosmosService.class).list().stream()
            .flatMap(m -> m.getCosmosDBAccountModule().list().stream())
            .filter(a -> a instanceof SqlCosmosDBAccount)
            .flatMap(a -> ((SqlCosmosDBAccount) a).sqlDatabases().list().stream())
            .map(this::define).collect(Collectors.toList());
    }

    @Override
    public AzureFormJPanel<Resource<SqlDatabase>> getResourcePanel(Project project) {
        final Function<Subscription, ? extends List<SqlCosmosDBAccount>> accountLoader = subscription ->
            Azure.az(AzureCosmosService.class).databaseAccounts(subscription.getId()).list().stream()
                .filter(account -> account instanceof SqlCosmosDBAccount)
                .map(account -> (SqlCosmosDBAccount) account).collect(Collectors.toList());
        final Function<SqlCosmosDBAccount, ? extends List<? extends SqlDatabase>> databaseLoader = account -> account.sqlDatabases().list();
        return new CosmosDatabaseResourcePanel<>(this, accountLoader, databaseLoader);
    }

    @Override
    public Map<String, String> initEnv(AzureServiceResource<SqlDatabase> data, Project project) {
        final SqlDatabase database = data.getData();
        final CosmosDBAccount account = database.getParent();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_ENDPOINT", Connection.ENV_PREFIX), account.getDocumentEndpoint());
        env.put(String.format("%s_KEY", Connection.ENV_PREFIX), account.listKeys().getPrimaryMasterKey());
        env.put(String.format("%s_DATABASE", Connection.ENV_PREFIX), database.getName());
        return env;
    }

    @Override
    public List<Pair<String, String>> getSpringProperties() {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.cloud.azure.cosmos.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.key", String.format("${%s_KEY}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.database", String.format("${%s_DATABASE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.populate-query-metrics", String.valueOf(true)));
        return properties;
    }

    @Nonnull
    @Override
    public String getResourceType() {
        return "DocumentDB";
    }

    @Nullable
    @Override
    public String getResourceConnectionString(@Nonnull SqlDatabase resource) {
        return resource.getModule().getParent().getCosmosDBAccountPrimaryConnectionString().getConnectionString();
    }
}
