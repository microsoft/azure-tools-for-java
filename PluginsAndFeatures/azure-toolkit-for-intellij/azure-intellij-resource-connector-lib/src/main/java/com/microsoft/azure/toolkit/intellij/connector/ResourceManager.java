/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface ResourceManager {

    static ResourceDefinition<? extends Resource> getDefinition(String type) {
        return Impl.definitions.get(type);
    }

    static List<ResourceDefinition<? extends Resource>> getDefinitions() {
        return new ArrayList<>(Impl.definitions.values());
    }

    static List<ResourceDefinition<? extends Resource>> getDefinitions(int role) {
        return Impl.definitions.values().stream()
                .filter(d -> (d.getRole() & role) == role)
                .collect(Collectors.toList());
    }

    static void registerDefinition(ResourceDefinition<? extends Resource> definition) {
        Impl.definitions.put(definition.getType(), definition);
    }

    void addResource(Resource resource);

    @Nullable
    Resource getResourceById(String id);

    @Log
    @State(name = Impl.ELEMENT_NAME_RESOURCES, storages = {@Storage("azure/connection-resources.xml")})
    final class Impl implements ResourceManager, PersistentStateComponent<Element> {
        protected static final String ELEMENT_NAME_RESOURCES = "resources";
        protected static final String ELEMENT_NAME_RESOURCE = "resource";
        protected final Set<Resource> resources = new LinkedHashSet<>();
        private static final Map<String, ResourceDefinition<? extends Resource>> definitions = new LinkedHashMap<>();

        @Override
        public synchronized void addResource(Resource resource) {
            resources.remove(resource);
            resources.add(resource);
        }

        @Nullable
        @Override
        public Resource getResourceById(String id) {
            return resources.stream().filter(e -> StringUtils.equals(e.getId(), id)).findFirst().orElse(null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Element getState() {
            final Element resourcesEle = new Element(ELEMENT_NAME_RESOURCES);
            for (final Resource resource : this.resources) {
                final ResourceDefinition<Resource> definition = (ResourceDefinition<Resource>) ResourceManager.getDefinition(resource.getType());
                final Element resourceEle = new Element(ELEMENT_NAME_RESOURCE);
                try {
                    if (definition.write(resourceEle, resource)) {
                        resourceEle.setAttribute(Resource.FIELD_TYPE, resource.getType());
                        resourcesEle.addContent(resourceEle);
                    }
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when persist resource of type '%s'", definition.getType()), e);
                }
            }
            return resourcesEle;
        }

        @Override
        public void loadState(@NotNull Element resourcesEle) {
            for (final Element resourceEle : resourcesEle.getChildren()) {
                final String resourceType = resourceEle.getAttributeValue(Resource.FIELD_TYPE);
                final ResourceDefinition<? extends Resource> definition = ResourceManager.getDefinition(resourceType);
                assert definition != null : String.format("definition for resource of type \"%s\" is not found", resourceType);
                try {
                    final Resource resource = definition.read(resourceEle);
                    if (Objects.nonNull(resource)) {
                        this.addResource(resource);
                    }
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when load a resource of type '%s'", resourceType), e);
                }
            }
        }
    }
}
