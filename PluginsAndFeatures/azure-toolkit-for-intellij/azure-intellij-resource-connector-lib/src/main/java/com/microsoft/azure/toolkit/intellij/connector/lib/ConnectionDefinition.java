/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.lib;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import lombok.Getter;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public abstract class ConnectionDefinition<R, C> {
    private final ResourceDefinition<R> resourceDefinition;
    private final ResourceDefinition<C> consumerDefinition;

    public ConnectionDefinition(ResourceDefinition<R> rd, ResourceDefinition<C> cd) {
        this.resourceDefinition = rd;
        this.consumerDefinition = cd;
    }

    public ConnectionDefinition(String rd, String cd) {
        this.resourceDefinition = (ResourceDefinition<R>) ResourceManager.getDefinition(rd);
        this.consumerDefinition = (ResourceDefinition<C>) ResourceManager.getDefinition(cd);
    }

    public final String getName() {
        return String.format("%s:%s", this.getResourceDefinition().getName(), this.getConsumerDefinition().getName());
    }

    /**
     * create {@link Connection} from given {@code resource} and {@code consumer}
     */
    @Nonnull
    public abstract Connection<R, C> define(Resource<R> resource, Resource<C> consumer);

    /**
     * read/deserialize a instance of {@link Connection} from {@code element}
     */
    @Nullable
    public abstract Connection<R, C> read(Element element);

    /**
     * write/serialize {@code connection} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    public abstract boolean write(Element element, Connection<? extends R, ? extends C> connection);

    /**
     * validate if the given {@code connection} is valid, e.g. check if
     * the given connection had already been created and persisted.
     *
     * @return false if the give {@code connection} is not valid and should not
     * be created and persisted.
     */
    public abstract boolean validate(Connection<?, ?> connection, Project project);

    /**
     * get <b>custom</b> connector dialog to create resource connection of
     * a type defined by this definition
     */
    @Nullable
    public AzureDialog<Connection<R, C>> getConnectorDialog() {
        return null;
    }
}
