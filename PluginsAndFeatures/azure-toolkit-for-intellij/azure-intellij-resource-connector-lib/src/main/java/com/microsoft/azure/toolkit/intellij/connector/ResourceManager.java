/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.java.Log;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface ResourceManager {

    static ResourceDefinition<?> getDefinition(String type) {
        return Impl.getDefinitions().get(type);
    }

    static List<ResourceDefinition<?>> getDefinitions() {
        return Impl.getDefinitions().values().stream()
            .sorted(Comparator.comparing(ResourceDefinition::getTitle))
            .collect(Collectors.toList());
    }

    static List<ResourceDefinition<?>> getDefinitions(int role) {
        return Impl.getDefinitions().values().stream()
            .filter(d -> (d.getRole() & role) == role)
            .sorted(Comparator.comparing(ResourceDefinition::getTitle))
            .collect(Collectors.toList());
    }

    void addResource(Resource<?> resource);

    @Nullable
    Resource<?> getResourceById(String id);

    @Log
    @State(name = Impl.ELEMENT_NAME_RESOURCES, storages = {@Storage("azure/connection-resources.xml")})
    final class Impl implements ResourceManager, PersistentStateComponent<Element> {
        private static final ExtensionPointName<ResourceDefinition<?>> exPoints =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.connectorResourceType");
        private static final String ATTR_DEFINITION = "type";
        protected static final String ELEMENT_NAME_RESOURCES = "resources";
        protected static final String ELEMENT_NAME_RESOURCE = "resource";
        protected final Set<Resource<?>> resources = new LinkedHashSet<>();
        private static Map<String, ResourceDefinition<?>> definitions;

        public synchronized static Map<String, ResourceDefinition<?>> getDefinitions() {
            if (MapUtils.isEmpty(definitions)) {
                definitions = exPoints.extensions().collect(Collectors.toMap(ResourceDefinition::getName, r -> r));
            }
            return definitions;
        }

        @Override
        public synchronized void addResource(Resource<?> resource) {
            resources.remove(resource);
            resources.add(resource);
        }

        @Nullable
        @Override
        public Resource<?> getResourceById(String id) {
            if (StringUtils.isBlank(id)) {
                return null;
            }
            return resources.stream().filter(e -> StringUtils.equals(e.getId(), id)).findFirst().orElse(null);
        }

        @Override
        @ExceptionNotification
        @AzureOperation(name = "connector.persist_connection_resources", type = AzureOperation.Type.ACTION)
        public Element getState() {
            final Element resourcesEle = new Element(ELEMENT_NAME_RESOURCES);
            this.resources.forEach(resource -> {
                final Element resourceEle = new Element(ELEMENT_NAME_RESOURCE);
                try {
                    if (resource.writeTo(resourceEle)) {
                        resourceEle.setAttribute(ATTR_DEFINITION, resource.getDefinition().getName());
                        resourcesEle.addContent(resourceEle);
                    }
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when persist resource of type '%s'", resource.getDefinition().getName()), e);
                }
            });
            return resourcesEle;
        }

        @Override
        @ExceptionNotification
        @AzureOperation(name = "connector.load_connection_resources", type = AzureOperation.Type.ACTION)
        public void loadState(@Nonnull Element resourcesEle) {
            for (final Element resourceEle : resourcesEle.getChildren()) {
                final String resDef = resourceEle.getAttributeValue(ATTR_DEFINITION);
                final ResourceDefinition<?> definition = ResourceManager.getDefinition(resDef);
                try {
                    Optional.ofNullable(definition).map(d -> definition.read(resourceEle)).ifPresent(this::addResource);
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when load a resource of type '%s'", resDef), e);
                }
            }
        }
    }
}
