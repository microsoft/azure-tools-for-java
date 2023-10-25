/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public interface FunctionSupported<T> extends ResourceDefinition<T> {
    @Nonnull
    String getResourceType();

    @Nullable
    String getResourceConnectionString(@Nonnull T resource);

    default Map<String, String> getPropertiesForFunction(@Nonnull T resource, @Nonnull Connection connection) {
        final String property = getFunctionProperty(connection);
        return Collections.singletonMap(property, getResourceConnectionString(resource));
    }

    default String getFunctionProperty(@Nonnull Connection connection) {
        return connection.getEnvPrefix();
    }
}
