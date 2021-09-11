/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ConnectionDefinition<R extends Resource, C extends Resource> {
    /**
     * create {@link Connection} from given {@code resource} and {@code consumer}
     */
    @Nonnull
    Connection<R, C> create(Resource resource, Resource consumer);

    /**
     * read/deserialize a instance of {@link Connection} from {@code element}
     */
    @Nullable
    Connection<R, C> read(Element element);

    /**
     * write/serialize {@code connection} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    boolean write(Element element, Connection<? extends R, ? extends C> connection);

    /**
     * validate if the given {@code connection} is valid, e.g. check if
     * the given connection had already been created and persisted.
     *
     * @return false if the give {@code connection} is not valid and should not
     * be created and persisted.
     */
    boolean validate(Connection<? extends Resource, ? extends Resource> connection, Project project);

    /**
     * get <b>custom</b> connector dialog to create resource connection of
     * a type defined by this definition
     */
    @Nullable
    default AzureDialog<Connection<R, C>> getConnectorDialog() {
        return null;
    }
}
