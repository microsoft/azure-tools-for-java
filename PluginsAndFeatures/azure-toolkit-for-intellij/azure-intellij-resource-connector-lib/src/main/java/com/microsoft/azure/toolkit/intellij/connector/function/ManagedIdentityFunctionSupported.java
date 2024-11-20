/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.function;

import com.microsoft.azure.toolkit.intellij.connector.Connection;

import javax.annotation.Nonnull;
import java.util.Map;

public interface ManagedIdentityFunctionSupported<T> extends FunctionSupported<T> {
    Map<String, String> getPropertiesForIdentityFunction(@Nonnull Connection<?, ?> connection);
}
