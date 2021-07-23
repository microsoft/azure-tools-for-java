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
import java.util.ArrayList;
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

    @Nonnull
    static ArrayList<ConnectionDefinition<? extends Resource, ? extends Resource>> getDefinitions() {
        return new ArrayList<>(Impl.definitions.values());
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

    @Nonnull
    @SuppressWarnings({"unchecked"})
    private static <R extends Resource, C extends Resource> ConnectionDefinition<R, C> getDefinitionOrDefault(String connectionType) {
        final ConnectionDefinition<R, C> definition = (ConnectionDefinition<R, C>) Impl.definitions.get(connectionType);
        return Optional.ofNullable(definition).orElse(new DefaultConnection.Definition<>());
    }

    static <R extends Resource, C extends Resource> void registerDefinition(String resourceType, String consumerType, ConnectionDefinition<R, C> definition) {
        final String type = typeOf(resourceType, consumerType);
        Impl.definitions.put(type, definition);
    }

    void addConnection(Connection<? extends Resource, ? extends Resource> connection);

    void removeConnection(String resourceId, String consumerId);

    List<Connection<? extends Resource, ? extends Resource>> getConnections();

    List<Connection<? extends Resource, ? extends Resource>> getConnectionsByResourceId(String id);

    List<Connection<? extends Resource, ? extends Resource>> getConnectionsByConsumerId(String id);

    @Log
    @State(name = Impl.ELEMENT_NAME_CONNECTIONS, storages = {@Storage("azure/resource-connections.xml")})
    final class Impl implements ConnectionManager, PersistentStateComponent<Element> {
        private static final String ELEMENT_NAME_CONNECTIONS = "connections";
        private static final String ELEMENT_NAME_CONNECTION = "connection";
        private static final String FIELD_TYPE = "type";
        private final Set<Connection<? extends Resource, ? extends Resource>> connections = new LinkedHashSet<>();
        private static final Map<String, ConnectionDefinition<? extends Resource, ? extends Resource>> definitions = new LinkedHashMap<>();

        static {
            definitions.put("default", new DefaultConnection.Definition<>());
        }

        @Override
        public synchronized void addConnection(Connection<? extends Resource, ? extends Resource> connection) {
            connections.removeIf(c -> Objects.equals(c, connection)); // always replace the old with the new one.
            connections.add(connection);
        }

        @Override
        public synchronized void removeConnection(String resourceId, String consumerId) {
            connections.removeIf(c -> StringUtils.equals(resourceId, c.getResource().getId()) && StringUtils.equals(consumerId, c.getConsumer().getId()));
        }

        @Override
        public List<Connection<? extends Resource, ? extends Resource>> getConnections() {
            return new ArrayList<>(connections);
        }

        @Override
        public List<Connection<? extends Resource, ? extends Resource>> getConnectionsByResourceId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getResource().getId())).collect(Collectors.toList());
        }

        @Override
        public List<Connection<? extends Resource, ? extends Resource>> getConnectionsByConsumerId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getConsumer().getId())).collect(Collectors.toList());
        }

        @Override
        @SuppressWarnings({"rawtypes"})
        public Element getState() {
            final Element connectionsEle = new Element(ELEMENT_NAME_CONNECTIONS);
            for (final Connection connection : this.connections) {
                final Element connectionEle = new Element(ELEMENT_NAME_CONNECTION);
                this.writeConnection(connectionEle, connection);
                connectionsEle.addContent(connectionEle);
            }
            return connectionsEle;
        }

        @Override
        public void loadState(@NotNull Element connectionsEle) {
            for (final Element connectionEle : connectionsEle.getChildren()) {
                final String connectionType = connectionEle.getAttributeValue(FIELD_TYPE);
                this.readConnection(connectionEle, connectionType);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private void writeConnection(Element connectionEle, Connection connection) {
            final String customType = ConnectionManager.typeOf(connection.getResource().getType(), connection.getConsumer().getType());
            final String connectionType = connection instanceof DefaultConnection ? "default" : customType;
            final ConnectionDefinition<?, ?> definition = definitions.get(connectionType);
            assert definition != null : String.format("definition for connection of type \"%s\" is not found", connectionType);
            definition.write(connectionEle, connection);
            connectionEle.setAttribute(FIELD_TYPE, connectionType);
        }

        private void readConnection(Element connectionEle, String connectionType) {
            final ConnectionDefinition<?, ?> definition = definitions.get(connectionType);
            assert Objects.nonNull(definition) : String.format("Not found connection definition for %s", connectionType);
            try {
                final Connection<?, ?> connection = definition.read(connectionEle);
                if (Objects.nonNull(connection)) {
                    this.addConnection(connection);
                }
            } catch (final Exception e) {
                log.log(Level.WARNING, String.format("error occurs when load a resource connection of type '%s'", connectionType), e);
            }

        }
    }
}
