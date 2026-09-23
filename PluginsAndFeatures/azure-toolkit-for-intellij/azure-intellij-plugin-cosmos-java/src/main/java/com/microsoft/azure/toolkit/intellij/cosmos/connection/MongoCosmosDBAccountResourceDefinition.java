/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.cosmos.connection;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.cosmos.mongo.MongoDatabase;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MongoCosmosDBAccountResourceDefinition extends BaseMongoCosmosDBAccountResourceDefinition implements SpringSupported<MongoDatabase> {
    public static final MongoCosmosDBAccountResourceDefinition INSTANCE = new MongoCosmosDBAccountResourceDefinition();

    @Override
    public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.data.mongodb.database", String.format("${%s_DATABASE}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.data.mongodb.uri", String.format("${%s_CONNECTION_STRING}", Connection.ENV_PREFIX)));
        return properties;
    }
}
