/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.dotazure;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionDefinition;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule.CONNECTIONS_FILE;

@Slf4j
public class ConnectionManager {
    private static final ExtensionPointName<ConnectionDefinition<?, ?>> exPoints =
        ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.connectorConnectionType");
    private static final String ELEMENT_NAME_CONNECTIONS = "connections";
    private static final String ELEMENT_NAME_CONNECTION = "connection";
    private static final String FIELD_TYPE = "type";
    public static final String FIELD_ID = "id";
    private static Map<String, ConnectionDefinition<?, ?>> definitions = null;
    private final Set<Connection<?, ?>> connections = new LinkedHashSet<>();
    @Getter
    private final Profile profile;
    private VirtualFile connectionsFile;

    public ConnectionManager(@Nonnull final Profile profile) {
        this.profile = profile;
        this.reload();
    }

    public void reload() {
        try {
            this.load();
        } catch (final Exception e) {
            final Throwable root = ExceptionUtils.getRootCause(e);
            if (!(root instanceof ProcessCanceledException)) {
                throw new AzureToolkitRuntimeException(e);
            }
        }
    }

    public synchronized static Map<String, ConnectionDefinition<?, ?>> getDefinitionsMap() {
        if (MapUtils.isEmpty(definitions)) {
            definitions = exPoints.getExtensionList().stream().collect(Collectors.toMap(ConnectionDefinition::getName, r -> r));
        }
        return definitions;
    }

    @Nonnull
    public static ArrayList<ConnectionDefinition<?, ?>> getDefinitions() {
        return new ArrayList<>(getDefinitionsMap().values());
    }

    @Nullable
    public static synchronized ConnectionDefinition<?, ?> getDefinitionOrDefault(@Nonnull String name) {
        return getDefinitionsMap().computeIfAbsent(name, def -> {
            final String[] split = def.split(":");
            final ResourceDefinition<?> rd = ResourceManager.getDefinition(split[0]);
            final ResourceDefinition<?> cd = ResourceManager.getDefinition(split[1]);
            return new ConnectionDefinition<>(rd, cd);
        });
    }

    @Nonnull
    public static List<Connection<?, ?>> getConnectionForRunConfiguration(final RunConfiguration config) {
        final List<Connection<?, ?>> connections = AzureModule.createIfSupport(config)
            .map(AzureModule::getDefaultProfile)
            .map(Profile::getConnectionManager)
            .map(ConnectionManager::getConnections).orElse(Collections.emptyList());
        return connections.stream().filter(c -> c.isApplicableFor(config)).collect(Collectors.toList());
    }

    @Nonnull
    public static ConnectionDefinition<?, ?> getDefinitionOrDefault(@Nonnull ResourceDefinition<?> rd, @Nonnull ResourceDefinition<?> cd) {
        final String name = getName(rd, cd);
        return getDefinitionsMap().computeIfAbsent(name, def -> new ConnectionDefinition<>(rd, cd));
    }

    @EqualsAndHashCode.Include
    public static String getName(ConnectionDefinition<?, ?> definition) {
        return getName(definition.getResourceDefinition(), definition.getConsumerDefinition());
    }

    public static String getName(@Nonnull ResourceDefinition<?> rd, @Nonnull ResourceDefinition<?> cd) {
        return String.format("%s:%s", rd.getName(), cd.getName());
    }

    @AzureOperation(name = "internal/connector.add_connection")
    public synchronized void addConnection(Connection<?, ?> connection) {
        connection.setProfile(this.profile);
        connections.add(connection);
    }

    @AzureOperation(name = "internal/connector.remove_connection")
    public synchronized void removeConnection(Connection<?, ?> connection) {
        connections.removeIf(c -> StringUtils.equals(connection.getId(), c.getId()));
    }

    public List<Connection<?, ?>> getConnections() {
        return new ArrayList<>(connections);
    }

    public List<Connection<?, ?>> getConnectionsByResourceId(String id) {
        return connections.stream().filter(e -> StringUtils.equals(id, e.getResource().getId())).collect(Collectors.toList());
    }

    public List<Connection<?, ?>> getConnectionsByConsumerId(String id) {
        return connections.stream().filter(e -> StringUtils.equals(id, e.getConsumer().getId())).collect(Collectors.toList());
    }

    @Nullable
    public VirtualFile getConnectionsFile() {
        return this.profile.getProfileDir().findChild(CONNECTIONS_FILE);
    }

    @ExceptionNotification
    @AzureOperation(name = "boundary/connector.save_connections")
    synchronized void save() throws IOException {
        final Element connectionsEle = new Element(ELEMENT_NAME_CONNECTIONS);
        for (final Connection<?, ?> connection : this.connections) {
            final Element connectionEle = new Element(ELEMENT_NAME_CONNECTION);
            connectionEle.setAttribute(FIELD_ID, connection.getId());
            connectionEle.setAttribute(FIELD_TYPE, ConnectionManager.getName(connection.getDefinition()));
            connection.write(connectionEle);
            connectionsEle.addContent(connectionEle);
        }
        if (Objects.isNull(connectionsFile)) {
            this.connectionsFile = this.profile.getProfileDir().findOrCreateChildData(this, CONNECTIONS_FILE);
        }
        JDOMUtil.write(connectionsEle, connectionsFile.toNioPath());
    }

    @ExceptionNotification
    @AzureOperation(name = "boundary/connector.load_connections")
    synchronized void load() throws Exception {
        this.connectionsFile = getConnectionsFile();
        if (Objects.isNull(connectionsFile) || connectionsFile.contentsToByteArray().length < 1) {
            return;
        }
        final Element connectionsEle = JDOMUtil.load(connectionsFile.toNioPath());
        final Profile profile = this.getProfile();
        final ResourceManager resourceManager = profile.getResourceManager();
        this.connections.clear();
        for (final Element connectionEle : connectionsEle.getChildren()) {
            final String name = connectionEle.getAttributeValue(FIELD_TYPE);
            final ConnectionDefinition<?, ?> definition = ConnectionManager.getDefinitionOrDefault(name);
            try {
                Optional.ofNullable(definition).map(d -> d.read(resourceManager, connectionEle)).ifPresent(this::addConnection);
            } catch (final Exception e) {
                log.warn(String.format("error occurs when load a resource connection of type '%s'", name), e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public String getNewPrefix(Resource resource, Resource consumer) {
        final Set<String> prefixes = this.getConnectionsByConsumerId(consumer.getId()).stream().map(Connection::getEnvPrefix).collect(Collectors.toSet());
        String prefix = resource.getDefinition().getDefaultEnvPrefix();
        int i = 1;
        while (prefixes.contains(prefix)) {
            prefix = prefix + (i++);
        }
        return prefix;
    }
}
