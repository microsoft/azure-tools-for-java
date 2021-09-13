/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class ConnectionDefinition<R, C> {
    @Nonnull
    private final ResourceDefinition<R> resourceDefinition;
    @Nonnull
    private final ResourceDefinition<C> consumerDefinition;

    public ConnectionDefinition(@Nonnull ResourceDefinition<R> rd, @Nonnull ResourceDefinition<C> cd) {
        this.resourceDefinition = rd;
        this.consumerDefinition = cd;
    }

    @EqualsAndHashCode.Include
    public final String getName() {
        return getConnectionName(this.resourceDefinition, this.consumerDefinition);
    }

    public static String getConnectionName(@Nonnull ResourceDefinition<?> rd, @Nonnull ResourceDefinition<?> cd) {
        return String.format("%s:%s", rd.getName(), cd.getName());
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
