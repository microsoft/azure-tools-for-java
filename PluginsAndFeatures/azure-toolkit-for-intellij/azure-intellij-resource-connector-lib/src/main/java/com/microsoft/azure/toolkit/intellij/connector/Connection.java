/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public interface Connection<R extends Resource, C extends Resource> {
    String FIELD_TYPE = "type";

    R getResource();

    C getConsumer();

    void beforeRun(@NotNull RunConfiguration configuration, DataContext dataContext);

    default String getType() {
        return typeOf(this.getResource(), this.getConsumer());
    }

    default void setType(String type) {
        assert StringUtils.equals(getType(), type) : String.format("incompatible resource type \"%s\":\"%s\"", getType(), type);
    }

    static String typeOf(Resource resource, Resource consumer) {
        return typeOf(resource.getDefinition().getType(), consumer.getDefinition().getType());
    }

    static String typeOf(String resourceType, String consumerType) {
        return String.format("%s:%s", resourceType, consumerType);
    }

}
