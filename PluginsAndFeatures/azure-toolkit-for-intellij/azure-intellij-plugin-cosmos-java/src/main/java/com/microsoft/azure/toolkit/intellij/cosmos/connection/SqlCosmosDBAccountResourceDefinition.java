/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.AuthenticationType;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringManagedIdentitySupported;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.cosmos.CosmosDBAccount;
import com.microsoft.azure.toolkit.lib.cosmos.sql.SqlDatabase;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;

public class SqlCosmosDBAccountResourceDefinition extends BaseSqlCosmosDBAccountResourceDefinition
        implements SpringSupported<SqlDatabase>, SpringManagedIdentitySupported<SqlDatabase> {

    public static final SqlCosmosDBAccountResourceDefinition INSTANCE = new SqlCosmosDBAccountResourceDefinition();

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.cloud.azure.cosmos.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.key", String.format("${%s_KEY}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.database", String.format("${%s_DATABASE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.populate-query-metrics", String.valueOf(true)));
        return properties;
    }



    @Override
    public Map<String, String> initIdentityEnv(Connection<SqlDatabase, ?> data, Project project) {
        final SqlDatabase database = data.getResource().getData();
        final CosmosDBAccount account = database.getParent();
        final HashMap<String, String> env = new HashMap<>();
        env.put(String.format("%s_ENDPOINT", Connection.ENV_PREFIX), account.getDocumentEndpoint());
        env.put(String.format("%s_DATABASE", Connection.ENV_PREFIX), database.getName());
        if (data.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            Optional.ofNullable(data.getUserAssignedManagedIdentity()).map(Resource::getData).ifPresent(identity -> {
                env.put(String.format("%s_CLIENT_ID", Connection.ENV_PREFIX), identity.getClientId());
                env.put("AZURE_CLIENT_ID", identity.getClientId());
            });
        }
        return env;
    }

    @Override
    public List<String> getRequiredPermissions() {
        return List.of("Microsoft.DocumentDB/databaseAccounts/readMetadata",
                "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/items/*",
                "Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/*");
    }

    @Nullable
    @Override
    public Map<String, BuiltInRole> getBuiltInRoles() {
        return null;
    }

    @Override
    public List<Pair<String, String>> getSpringPropertiesForManagedIdentity(String key, Connection<?, ?> connection) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.cloud.azure.cosmos.endpoint", String.format("${%s_ENDPOINT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cloud.azure.cosmos.database", String.format("${%s_DATABASE}", Connection.ENV_PREFIX)));
        // properties.add(Pair.of("spring.cloud.azure.cosmos.credential.managed-identity-enabled", String.valueOf(true)));
        if (connection.getAuthenticationType() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            properties.add(Pair.of("spring.cloud.azure.cosmos.credential.client-id", String.format("${%s_CLIENT_ID}", Connection.ENV_PREFIX)));
        }
        return properties;
    }
}
