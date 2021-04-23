/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class DefaultConnection<R extends Resource, C extends Resource> implements Connection<R, C> {
    private final R resource;
    private final C consumer;

    @Override
    public boolean prepareBeforeRun(@NotNull RunConfiguration configuration, DataContext dataContext) {
        // do nothing
        return true;
    }

    @Getter
    public static class Definition<R extends Resource, C extends Resource> implements ConnectionDefinition<R, C> {

        @Override
        public Connection<R, C> create(R resource, C consumer) {
            return new DefaultConnection<>(resource, consumer);
        }

        @Override
        public Connection<R, C> read(Element connectionEle) {
            final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
            final R resource = (R) manager.getResourceById(connectionEle.getChildTextTrim("resource"));
            final Element consumerEle = connectionEle.getChild("consumer");
            final C consumer;
            if (ModuleResource.TYPE.equals(consumerEle.getAttributeValue("type"))) {
                consumer = (C) new ModuleResource(consumerEle.getTextTrim());
            } else {
                consumer = (C) manager.getResourceById(connectionEle.getChildTextTrim("consumer"));
            }
            return new DefaultConnection<>(resource, consumer);
        }

        @Override
        public boolean write(Element connectionEle, Connection<? extends R, ? extends C> connection) {
            final R resource = connection.getResource();
            final C consumer = connection.getConsumer();
            connectionEle.addContent(new Element("resource").setAttribute("type", resource.getType()).setText(resource.getId()));
            connectionEle.addContent(new Element("consumer").setAttribute("type", consumer.getType()).setText(consumer.getId()));
            return true;
        }

        @Override
        public boolean validate(Connection<R, C> connection, Project project) {
            return true;
        }
    }
}
