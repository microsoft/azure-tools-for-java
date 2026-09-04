/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.cosmos.cassandra.CassandraKeyspace;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CassandraCosmosDBAccountResourceDefinition extends BaseCassandraCosmosDBAccountResourceDefinition implements SpringSupported<CassandraKeyspace> {
    public static final CassandraCosmosDBAccountResourceDefinition INSTANCE = new CassandraCosmosDBAccountResourceDefinition();

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.cassandra.contact-points", String.format("${%s_CONTACT_POINT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cassandra.port", String.format("${%s_PORT}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cassandra.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cassandra.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cassandra.keyspace-name", String.format("${%s_KEYSPACE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.cassandra.schema-action", "create_if_not_exists"));
        properties.add(Pair.of("spring.cassandra.ssl.enabled", "true"));
        properties.add(Pair.of("spring.cassandra.local-datacenter", String.format("${%s_REGION}", Connection.ENV_PREFIX)));
        return properties;
    }
}
