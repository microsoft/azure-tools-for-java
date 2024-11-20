/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.function;

import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public interface FunctionSupported<T> extends ResourceDefinition<T> {
    @Nonnull
    String getResourceType();

    @Nullable
    String getResourceConnectionString(@Nonnull T resource);

    default Map<String, String> getPropertiesForFunction(@Nonnull Connection<?,?> connection) {
        final T data = (T) connection.getResource().getData();
        return Objects.isNull(data) ? Collections.emptyMap() :
                Collections.singletonMap(getFunctionProperty(connection), getResourceConnectionString(data));
    }

    default String getFunctionProperty(@Nonnull Connection<?,?> connection) {
        return connection.getEnvPrefix();
    }
}
