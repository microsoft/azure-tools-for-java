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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public interface ConnectionManager extends PersistentStateComponent<Element> {
    private static String typeOf(String resourceType, String consumerType) {
        return String.format("%s:%s", resourceType, consumerType);
    }

    @Nullable
    @SuppressWarnings({"unchecked"})
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinition(String resourceType, String consumerType) {
        final String type = typeOf(resourceType, consumerType);
        return ((ConnectionDefinition<R, C>) Impl.definitions.get(type));
    }

    @Nonnull
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinitionOrDefault(String resourceType, String consumerType) {
        final ConnectionDefinition<R, C> definition = ConnectionManager.getDefinition(resourceType, consumerType);
        return Optional.ofNullable(definition).orElse(new DefaultConnection.Definition<>());
    }

    static <R extends Resource, C extends Resource> void registerDefinition(String resourceType, String consumerType, ConnectionDefinition<R, C> definition) {
        final String type = typeOf(resourceType, consumerType);
        Impl.definitions.put(type, definition);
    }

    void addConnection(Connection<? extends Resource, ? extends Resource> connection);

    List<Connection<? extends Resource, ? extends Resource>> getConnectionsByResourceId(String id);

    List<Connection<? extends Resource, ? extends Resource>> getConnectionsByConsumerId(String id);

    @State(name = Impl.ELEMENT_NAME_CONNECTIONS, storages = {@Storage("azure/resource-connections.xml")})
    @Log
    final class Impl implements ConnectionManager, PersistentStateComponent<Element> {
        protected static final String ELEMENT_NAME_CONNECTIONS = "connections";
        protected static final String ELEMENT_NAME_CONNECTION = "connection";

        private final Set<Connection<? extends Resource, ? extends Resource>> connections = new LinkedHashSet<>();
        private static final Map<String, ConnectionDefinition<? extends Resource, ? extends Resource>> definitions = new LinkedHashMap<>();

        @Override
        public synchronized void addConnection(Connection<? extends Resource, ? extends Resource> connection) {
            connections.add(connection);
        }

        @Override
        public List<Connection<? extends Resource, ? extends Resource>> getConnectionsByResourceId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getResource().getId())).collect(Collectors.toList());
        }

        @Override
        public List<Connection<? extends Resource, ? extends Resource>> getConnectionsByConsumerId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getConsumer().getId())).collect(Collectors.toList());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Element getState() {
            final Element connectionsEle = new Element(ELEMENT_NAME_CONNECTIONS);
            for (final Connection connection : this.connections) {
                final String connectionType = ConnectionManager.typeOf(connection.getResource().getType(), connection.getConsumer().getType());
                final var definition = definitions.get(connectionType);
                assert definition != null : String.format("definition for connection of type \"%s\" is not found", connectionType);
                final Element connectionEle = new Element(ELEMENT_NAME_CONNECTION);
                try {
                    definition.write(connectionEle, connection);
                    connectionEle.setAttribute(Connection.FIELD_TYPE, connectionType);
                    connectionsEle.addContent(connectionEle);
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when persist a resource connection of type '%s'", connectionType), e);
                }
            }
            return connectionsEle;
        }

        public void loadState(@NotNull Element connectionsEle) {
            for (final Element connectionEle : connectionsEle.getChildren()) {
                final String connectionType = connectionEle.getAttributeValue(Connection.FIELD_TYPE);
                final ConnectionDefinition<? extends Resource, ? extends Resource> definition = definitions.get(connectionType);
                assert definition != null : String.format("definition for connection of type \"%s\" is not found", connectionType);
                try {
                    final var connection = definition.read(connectionEle);
                    if (Objects.nonNull(connection)) {
                        this.addConnection(connection);
                    }
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when load a resource connection of type '%s'", connectionType), e);
                }
            }
        }
    }
}
