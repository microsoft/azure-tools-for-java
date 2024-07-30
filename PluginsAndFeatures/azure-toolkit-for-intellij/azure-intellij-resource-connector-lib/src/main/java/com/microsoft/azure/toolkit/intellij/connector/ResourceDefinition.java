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
import java.util.ArrayList;
import java.util.List;

public interface ResourceDefinition<T> {
    int RESOURCE = 1;
    int CONSUMER = 2;
    int IDENTITY = 4;
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

    default Resource<T> define(T resource) {
        return this.define(resource, null);
    }

    Resource<T> define(T resource, String id);

    /**
     * get resource selection panel<br>
     * with this panel, user could select/create a {@link T} resource.
     */
    AzureFormJPanel<Resource<T>> getResourcePanel(final Project project);

    /**
     * get candidate resources
     */
    List<Resource<T>> getResources(Project project);

    /**
     * write/serialize {@code resource} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    boolean write(@Nonnull final Element element, @Nonnull final Resource<T> resource);

    /**
     * read/deserialize a instance of {@link T} from {@code element}
     */
    @Nullable
    Resource<T> read(@Nonnull final Element element);

    default boolean isEnvPrefixSupported() {
        return true;
    }

    default String getDefaultEnvPrefix() {
        return this.getName().toUpperCase().replaceAll("[^a-zA-Z0-9]", "_");
    }

    default List<AuthenticationType> getSupportedAuthenticationTypes() {
        final List<AuthenticationType> result = new ArrayList<>();
        result.add(AuthenticationType.CONNECTION_STRING);
        if (this instanceof IManagedIdentitySupported) {
            result.add(AuthenticationType.SYSTEM_ASSIGNED_MANAGED_IDENTITY);
            result.add(AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY);
        }
        return result;
    }
}
