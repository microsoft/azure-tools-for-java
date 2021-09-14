/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ResourceDefinition<T> {
    int RESOURCE = 1;
    int CONSUMER = 2;
    int BOTH = RESOURCE | CONSUMER;

    /**
     * get the role of the resource
     *
     * @return {@link ResourceDefinition#RESOURCE RESOURCE=1} if this resource can only be consumed,<br>
     * {@link ResourceDefinition#CONSUMER CONSUMER=2} if this resource can only be a consumer or<br>
     * {@link ResourceDefinition#BOTH BOTH=3} if this resource can be both resource and consumer
     */
    default int getRole() {
        return RESOURCE;
    }

    default String getTitle() {
        return this.getName();
    }

    @Nullable
    default String getIcon() {
        return null;
    }

    String getName();

    Resource<T> define(T resource);

    /**
     * get resource selection panel<br>
     * with this panel, user could select/create a {@link T} resource.
     *
     * @param type type of the resource
     */
    AzureFormJPanel<T> getResourcesPanel(@Nonnull final String type, final Project project);

    /**
     * write/serialize {@code resouce} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    boolean write(@Nonnull final Element element, @Nonnull final Resource<T> resource);

    /**
     * read/deserialize a instance of {@link T} from {@code element}
     */
    Resource<T> read(@Nonnull final Element element);
}
