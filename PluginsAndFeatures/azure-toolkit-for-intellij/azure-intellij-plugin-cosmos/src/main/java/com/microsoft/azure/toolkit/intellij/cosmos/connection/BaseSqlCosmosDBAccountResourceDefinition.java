package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.AuthenticationType;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.function.ManagedIdentityFunctionSupported;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.cosmos.AzureCosmosService;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlCosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseSqlCosmosDBAccountResourceDefinition extends AzureServiceResource.Definition<SqlDatabase>
        implements ManagedIdentityFunctionSupported<SqlDatabase> {

    public BaseSqlCosmosDBAccountResourceDefinition() {
        super("Azure.Cosmos.Sql", "Azure Cosmos DB account (SQL)", AzureIcons.Cosmos.MODULE.getIconPath());
    }

    @Override
    public SqlDatabase getResource(String dataId, final String id) {
        return Azure.az(AzureCosmosService.class).getById(dataId);
    }

    @Override
    public List<Resource<SqlDatabase>> getResources(Project project) {
        return Azure.az(AzureCosmosService.class).list().stream()
            .flatMap(m -> m.getCosmosDBAccountModule().list().stream())
            .filter(a -> a instanceof SqlCosmosDBAccount)
            .flatMap(s -> {
                try {
                    return ((SqlCosmosDBAccount) s).sqlDatabases().list().stream();
                } catch (final Throwable e) {
                    return Stream.empty();
                }
            })
            .map(this::define).toList();
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

    @Override
    public Map<String, String> getPropertiesForIdentityFunction(@Nonnull Connection<?, ?> connection) {
        final Map<String, String> result = new HashMap<>();
        final SqlDatabase data = (SqlDatabase)connection.getResource().getData();
        // result.put(String.format("%s__accountName", connection.getEnvPrefix()), data.getName());
        result.put(String.format("%s__accountEndpoint", connection.getEnvPrefix()), data.getParent().getDocumentEndpoint());
        result.put(String.format("%s__credential", connection.getEnvPrefix()), "managedidentity");
        if (connection.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            Optional.ofNullable(connection.getUserAssignedManagedIdentity()).map(Resource::getData).ifPresent(identity -> {
                result.put(String.format("%s__clientId", connection.getEnvPrefix()), identity.getClientId());
            });
        }
        return result;
    }
}
