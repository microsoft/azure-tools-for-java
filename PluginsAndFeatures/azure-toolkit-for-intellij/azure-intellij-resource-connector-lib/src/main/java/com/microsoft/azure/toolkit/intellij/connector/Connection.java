/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * the <b>{@code resource connection}</b>
 *
 * @param <R> type of the resource consumed by {@link C}
 * @param <C> type of the consumer consuming {@link R},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
public interface Connection<R extends Resource, C extends Resource> {
    String FIELD_TYPE = "type";

    /**
     * @return the resource consumed by consumer
     */
    R getResource();

    /**
     * @return the consumer consuming resource
     */
    C getConsumer();

    /**
     * called before execute the {@code RunConfiguration} of connected module<br>
     * the connection can intervene the run configuration by e.g. setting environment variables
     */
    void beforeRun(@NotNull RunConfiguration configuration, DataContext dataContext);

}
