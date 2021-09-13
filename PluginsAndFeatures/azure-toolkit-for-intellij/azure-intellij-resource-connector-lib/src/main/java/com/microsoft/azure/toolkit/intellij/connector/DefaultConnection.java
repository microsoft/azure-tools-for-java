/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DefaultConnection<R, C> implements Connection<R, C> {
    @EqualsAndHashCode.Include
    private final Resource<R> resource;
    @EqualsAndHashCode.Include
    private final Resource<C> consumer;
    @EqualsAndHashCode.Include
    private final Definition<R, C> definition;

    public DefaultConnection(Resource<R> resource, Resource<C> consumer) {
        this.resource = resource;
        this.consumer = consumer;
        this.definition = new Definition<>(resource.getDefinition(), consumer.getDefinition());
    }

    @Override
    public boolean prepareBeforeRun(@NotNull RunConfiguration configuration, DataContext dataContext) {
        // do nothing
        return true;
    }

    @Getter
    public static class Definition<R, C> extends ConnectionDefinition<R, C> {

        public Definition(ResourceDefinition<R> rd, ResourceDefinition<C> cd) {
            super(rd, cd);
        }

        @Override
        public Connection<R, C> define(Resource<R> resource, Resource<C> consumer) {
            return new DefaultConnection<>(resource, consumer, this);
        }

        @Override
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
                return new DefaultConnection<>(resource, consumer);
            }
            return null;
        }

        @Override
        public boolean write(Element connectionEle, Connection<? extends R, ? extends C> connection) {
            final Resource<? extends R> resource = connection.getResource();
            final Resource<? extends C> consumer = connection.getConsumer();
            connectionEle.addContent(new Element("resource")
                    .setAttribute("type", resource.getDefName())
                    .setText(resource.getId()));
            connectionEle.addContent(new Element("consumer")
                    .setAttribute("type", consumer.getDefName())
                    .setText(consumer.getId()));
            return true;
        }

        @Override
        public boolean validate(Connection<?, ?> connection, Project project) {
            return true;
        }
    }
}
