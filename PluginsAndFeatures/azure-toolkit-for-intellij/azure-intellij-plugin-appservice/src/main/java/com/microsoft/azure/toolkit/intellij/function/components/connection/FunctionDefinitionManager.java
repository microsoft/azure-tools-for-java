/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function.components.connection;

import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FunctionDefinitionManager {

    @Nullable
    public static FunctionSupported<?> getFunctionDefinitionByResourceType(@Nonnull String resourceType) {
        try {
            final ResourceDefinition<?> definition = ResourceManager.getDefinition(resourceType);
            return ResourceManager.getDefinitions().stream()
                    .filter(d -> d instanceof FunctionSupported)
                    .map(d -> (FunctionSupported) d)
                    .filter(d -> StringUtils.equals(d.getResourceType(), resourceType))
                    .findFirst().orElse(null);
        } catch (Throwable t) {
            // swallow exception when get connection definition
            return null;
        }
    }
}
