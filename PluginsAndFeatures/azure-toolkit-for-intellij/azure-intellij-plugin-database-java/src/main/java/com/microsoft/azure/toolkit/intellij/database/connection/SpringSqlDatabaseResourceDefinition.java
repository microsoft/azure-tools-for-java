/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.connection;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.spring.SpringSupported;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public interface SpringSqlDatabaseResourceDefinition<T extends IDatabase> extends SpringSupported<T> {

    @Override
    default public List<Pair<String, String>> getSpringProperties(@Nullable final String key) {
        final List<Pair<String, String>> properties = new ArrayList<>();
        properties.add(Pair.of("spring.datasource.url", String.format("${%s_URL}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.username", String.format("${%s_USERNAME}", Connection.ENV_PREFIX)));
        properties.add(Pair.of("spring.datasource.password", String.format("${%s_PASSWORD}", Connection.ENV_PREFIX)));
        return properties;
    }
}
