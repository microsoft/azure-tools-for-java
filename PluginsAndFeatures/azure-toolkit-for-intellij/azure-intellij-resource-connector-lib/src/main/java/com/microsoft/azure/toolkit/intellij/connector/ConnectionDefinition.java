/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConnectionDefinition<R, C> {
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
    public Connection<R, C> define(Resource<R> resource, Resource<C> consumer) {
        return new Connection<>(resource, consumer, this);
    }

    /**
     * read/deserialize a instance of {@link Connection} from {@code element}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public Connection<R, C> read(Element connectionEle) {
        final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
        final Element consumerEle = connectionEle.getChild("consumer");
        final Element resourceEle = connectionEle.getChild("resource");
        final String consumerDefName = consumerEle.getAttributeValue("type");
        final Resource<R> resource = (Resource<R>) manager.getResourceById(resourceEle.getTextTrim());
        final Resource<C> consumer = ModuleResource.Definition.IJ_MODULE.getName().equals(consumerDefName) ?
                (Resource<C>) new ModuleResource(consumerEle.getTextTrim()) :
                (Resource<C>) manager.getResourceById(consumerEle.getTextTrim());
        if (Objects.nonNull(resource) && Objects.nonNull(consumer)) {
            return this.define(resource, consumer);
        }
        return null;
    }

    /**
     * write/serialize {@code connection} to {@code element} for persistence
     *
     * @return true if to persist, false otherwise
     */
    public boolean write(Element connectionEle, Connection<? extends R, ? extends C> connection) {
        final Resource<? extends R> resource = connection.getResource();
        final Resource<? extends C> consumer = connection.getConsumer();
        connectionEle.addContent(new Element("resource")
                .setAttribute("type", resource.getDefinition().getName())
                .setText(resource.getId()));
        connectionEle.addContent(new Element("consumer")
                .setAttribute("type", resource.getDefinition().getName())
                .setText(consumer.getId()));
        return true;
    }

    /**
     * validate if the given {@code connection} is valid, e.g. check if
     * the given connection had already been created and persisted.
     *
     * @return false if the give {@code connection} is not valid and should not
     * be created and persisted.
     */
    public boolean validate(Connection<?, ?> connection, Project project) {
        return true;
    }

    /**
     * get <b>custom</b> connector dialog to create resource connection of
     * a type defined by this definition
     */
    @Nullable
    public AzureDialog<Connection<R, C>> getConnectorDialog() {
        return null;
    }
}
