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

    @Nullable
    @SuppressWarnings({"unchecked"})
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinition(String type) {
        return ((ConnectionDefinition<R, C>) Impl.definitions.get(type));
    }

    @Nullable
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinitionOrDefault(String type) {
        final ConnectionDefinition<R, C> definition = getDefinition(type);
        return Optional.ofNullable(definition).orElse(new DefaultConnection.Definition<>(type));
    }

    @Nullable
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinition(R resource, C consumer) {
        final String type = Connection.typeOf(resource, consumer);
        return getDefinition(type);
    }

    @Nonnull
    static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinitionOrDefault(R resource, C consumer) {
        final String type = Connection.typeOf(resource, consumer);
        return Optional.ofNullable(ConnectionManager.getDefinition(resource, consumer)).orElse(new DefaultConnection.Definition<>(type));
    }

    static <R extends Resource, C extends Resource> void registerDefinition(String type, ConnectionDefinition<R, C> definition) {
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
                final var definition = (ConnectionDefinition<? extends Resource, ? extends Resource>) ConnectionManager.getDefinition(connection.getType());
                assert definition != null : String.format("definition for connection of type \"%s\" is not found", connection.getType());
                final Element connectionEle = new Element(ELEMENT_NAME_CONNECTION);
                try {
                    definition.write(connectionEle, connection);
                    connectionEle.setAttribute(Connection.FIELD_TYPE, connection.getType());
                    connectionsEle.addContent(connectionEle);
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when persist a resource connection of type '%s'", connection.getType()), e);
                }
            }
            return connectionsEle;
        }

        public void loadState(@NotNull Element connectionsEle) {
            for (final Element connectionEle : connectionsEle.getChildren()) {
                final String connectionType = connectionEle.getAttributeValue(Connection.FIELD_TYPE);
                final ConnectionDefinition<Resource, Resource> definition = ConnectionManager.getDefinition(connectionType);
                assert definition != null : String.format("definition for connection of type \"%s\" is not found", connectionType);
                try {
                    final var connection = definition.read(connectionEle);
                    if (Objects.nonNull(connection)) {
                        connection.setType(connectionType);
                        this.addConnection(connection);
                    }
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when load a resource connection of type '%s'", connectionType), e);
                }
            }
        }
    }
}
