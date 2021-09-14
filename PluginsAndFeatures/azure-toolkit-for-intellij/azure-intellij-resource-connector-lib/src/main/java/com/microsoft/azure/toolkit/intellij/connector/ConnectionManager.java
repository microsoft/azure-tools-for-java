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
    @Nonnull
    static ArrayList<ConnectionDefinition<?, ?>> getDefinitions() {
        return new ArrayList<>(Impl.definitions.values());
    }

    @Nullable
    static ConnectionDefinition<?, ?> getDefinition(@Nonnull ResourceDefinition<?> rd, @Nonnull ResourceDefinition<?> cd) {
        final String name = ConnectionDefinition.getConnectionName(rd, cd);
        return Impl.definitions.get(name);
    }

    @Nonnull
    static ConnectionDefinition<?, ?> getDefinitionOrDefault(@Nonnull ResourceDefinition<?> rd, @Nonnull ResourceDefinition<?> cd) {
        final ConnectionDefinition<?, ?> definition = ConnectionManager.getDefinition(rd, cd);
        if (Objects.isNull(definition)) {
            return new ConnectionDefinition<>(rd, cd);
        }
        return definition;
    }

    static <R, C> void registerDefinition(ConnectionDefinition<R, C> definition) {
        Impl.definitions.put(definition.getName(), definition);
    }

    void addConnection(Connection<?, ?> connection);

    void removeConnection(String resourceId, String consumerId);

    List<Connection<?, ?>> getConnections();

    List<Connection<?, ?>> getConnectionsByResourceId(String id);

    List<Connection<?, ?>> getConnectionsByConsumerId(String id);

    @Log
    @State(name = Impl.ELEMENT_NAME_CONNECTIONS, storages = {@Storage("azure/resource-connections.xml")})
    final class Impl implements ConnectionManager, PersistentStateComponent<Element> {
        private static final String ELEMENT_NAME_CONNECTIONS = "connections";
        private static final String ELEMENT_NAME_CONNECTION = "connection";
        private static final String FIELD_TYPE = "type";
        private final Set<Connection<?, ?>> connections = new LinkedHashSet<>();
        private static final Map<String, ConnectionDefinition<?, ?>> definitions = new LinkedHashMap<>();

        @Override
        public synchronized void addConnection(Connection<?, ?> connection) {
            connections.removeIf(c -> Objects.equals(c, connection)); // always replace the old with the new one.
            connections.add(connection);
        }

        @Override
        public synchronized void removeConnection(String resourceId, String consumerId) {
            connections.removeIf(c -> StringUtils.equals(resourceId, c.getResource().getId()) && StringUtils.equals(consumerId, c.getConsumer().getId()));
        }

        @Override
        public List<Connection<?, ?>> getConnections() {
            return new ArrayList<>(connections);
        }

        @Override
        public List<Connection<?, ?>> getConnectionsByResourceId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getResource().getId())).collect(Collectors.toList());
        }

        @Override
        public List<Connection<?, ?>> getConnectionsByConsumerId(String id) {
            return connections.stream().filter(e -> StringUtils.equals(id, e.getConsumer().getId())).collect(Collectors.toList());
        }

        @Override
        public Element getState() {
            final Element connectionsEle = new Element(ELEMENT_NAME_CONNECTIONS);
            for (final Connection<?, ?> connection : this.connections) {
                final Element connectionEle = new Element(ELEMENT_NAME_CONNECTION);
                connectionEle.setAttribute(FIELD_TYPE, connection.getDefinition().getName());
                connection.write(connectionEle);
                connectionsEle.addContent(connectionEle);
            }
            return connectionsEle;
        }

        @Override
        public void loadState(@NotNull Element connectionsEle) {
            for (final Element connectionEle : connectionsEle.getChildren()) {
                final String connectionType = connectionEle.getAttributeValue(FIELD_TYPE);
                final ConnectionDefinition<?, ?> definition = definitions.get(connectionType);
                try {
                    Optional.ofNullable(definition).map(d -> d.read(connectionEle)).ifPresent(this::addConnection);
                } catch (final Exception e) {
                    log.log(Level.WARNING, String.format("error occurs when load a resource connection of type '%s'", connectionType), e);
                }
            }
        }
    }
}
