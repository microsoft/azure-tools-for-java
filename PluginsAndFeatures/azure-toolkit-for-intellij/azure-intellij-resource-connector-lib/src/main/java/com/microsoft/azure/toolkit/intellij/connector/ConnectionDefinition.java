/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.identities.Identity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager.FIELD_ID;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConnectionDefinition<R, C> {
    private static final ExtensionPointName<ConnectionProvider> exPoints =
            ExtensionPointName.create("com.microsoft.tooling.msservices.intellij.azure.connectionProvider");
    public static final String ENV_PREFIX = "envPrefix";
    @Nonnull
    @EqualsAndHashCode.Include
    private final ResourceDefinition<R> resourceDefinition;
    @Nonnull
    @EqualsAndHashCode.Include
    private final ResourceDefinition<C> consumerDefinition;
    private static final String PROMPT_TITLE = "Azure Resource Connector";

    public ConnectionDefinition(@Nonnull ResourceDefinition<R> rd, @Nonnull ResourceDefinition<C> cd) {
        this.resourceDefinition = rd;
        this.consumerDefinition = cd;
    }

    public String getName() {
        return String.format("%s:%s", resourceDefinition.getName(), consumerDefinition.getName());
    }

    /**
     * create {@link Connection} from given {@code resource} and {@code consumer}
     */
    @Nonnull
    public Connection<R, C> define(Resource<R> resource, Resource<C> consumer) {
        return define(UUID.randomUUID().toString(), resource, consumer);
    }

    public Connection<R, C> define(String id, Resource<R> resource, Resource<C> consumer) {
        // todo: @Miller @hanli get connection provider by connection type, eg: Java, Csharp...
        return getConnectionProvider().define(id, resource, consumer, this);
    }

    private ConnectionProvider getConnectionProvider() {
        return exPoints.getExtensionList().stream().findFirst().orElse(Connection::new);
    }

    /**
     * read/deserialize a instance of {@link Connection} from {@code element}
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public Connection<R, C> read(@Nonnull final com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager manager, Element connectionEle) {
        final String id = connectionEle.getAttributeValue(FIELD_ID);
        final Resource<R> resource = Optional.ofNullable(readResourceFromElement(connectionEle, manager))
                .orElseGet(() -> new InvalidResource<>(this.getResourceDefinition()));
        final Resource<C> consumer = Optional.ofNullable(readConsumerFromElement(connectionEle, manager))
                .orElseGet(() -> new InvalidResource<>(this.getConsumerDefinition()));
        final Connection<R, C> connection = this.define(id, resource, consumer);
        Optional.ofNullable(connectionEle.getChild("identity"))
                .map(ele -> (Resource<Identity>) manager.getResourceById(ele.getTextTrim()))
                .ifPresent(connection::setUserAssignedManagedIdentity);
        connection.setEnvPrefix(connectionEle.getAttributeValue(ENV_PREFIX));

        final AuthenticationType authentication = Optional.ofNullable(connectionEle.getAttributeValue("authentication"))
                .filter(StringUtils::isNotBlank).map(AuthenticationType::valueOf).orElse(AuthenticationType.CONNECTION_STRING);
        connection.setAuthenticationType(authentication);
        return connection;
    }

    private Resource<R> readResourceFromElement(Element connectionEle, @Nonnull final com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager manager) {
        final Element resourceEle = connectionEle.getChild("resource");
        return (Resource<R>) manager.getResourceById(resourceEle.getTextTrim());
    }

    private Resource<C> readConsumerFromElement(Element connectionEle, @Nonnull final com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager manager) {
        final Element consumerEle = connectionEle.getChild("consumer");
        final String consumerDefName = consumerEle.getAttributeValue("type");
        return ModuleResource.Definition.IJ_MODULE.getName().equals(consumerDefName) ?
                (Resource<C>) new ModuleResource(consumerEle.getTextTrim()) :
                (Resource<C>) manager.getResourceById(consumerEle.getTextTrim());
    }

    public Connection<R, C> readDeprecatedConnection(Element connectionEle) {
        final ResourceManager manager = ServiceManager.getService(ResourceManager.class);
        final Element consumerEle = connectionEle.getChild("consumer");
        final Element resourceEle = connectionEle.getChild("resource");
        final String consumerDefName = consumerEle.getAttributeValue("type");
        final Resource<R> resource = (Resource<R>) manager.getResourceById(resourceEle.getTextTrim());
        final Resource<C> consumer = ModuleResource.Definition.IJ_MODULE.getName().equals(consumerDefName) ?
                (Resource<C>) new ModuleResource(consumerEle.getTextTrim()) :
                (Resource<C>) manager.getResourceById(consumerEle.getTextTrim());
        if (Objects.nonNull(resource) && Objects.nonNull(consumer)) {
            final Connection<R, C> connection = this.define(resource, consumer);
            connection.setEnvPrefix(connectionEle.getAttributeValue("envPrefix"));
            return connection;
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
                .setAttribute("type", consumer.getDefinition().getName())
                .setText(consumer.getId()));
        final Resource<Identity> userAssignedManagedIdentity = connection.getUserAssignedManagedIdentity();
        if (Objects.nonNull(userAssignedManagedIdentity)) {
            connectionEle.addContent(new Element("identity")
                    .setAttribute("type", userAssignedManagedIdentity.getDefinition().getName())
                    .setText(userAssignedManagedIdentity.getId()));
        }
        connectionEle.setAttribute("envPrefix", connection.getEnvPrefix());
        connectionEle.setAttribute("authentication", String.valueOf(connection.getAuthenticationType()));
        return true;
    }

    /**
     * validate if the given {@code connection} is valid, e.g. check if
     * the given connection had already been created and persisted.
     *
     * @return false if the give {@code connection} is not valid and should not
     * be created and persisted.
     */
    public boolean validate(Connection<R, C> connection, Project project) {
        final Resource<C> consumer = connection.getConsumer();
        final Resource<R> resource = connection.getResource();
        final Profile profile = Optional.of(consumer)
                .filter(c -> c instanceof ModuleResource)
                .map(c -> ((ModuleResource) c).getModuleName())
                .map(ModuleManager.getInstance(project)::findModuleByName)
                .map(AzureModule::from)
                .map(AzureModule::getDefaultProfile)
                .orElse(null);
        if (Objects.isNull(profile)) {
            return true;
        }
        final Resource<R> existedResource = (Resource<R>) profile.getResourceManager().getResourceById(resource.getId());
        if (Objects.nonNull(existedResource)) { // not new
            final String current = resource.getDataId();
            final String origin = existedResource.getDataId();
            if (Objects.equals(origin, current) && existedResource.isModified(resource)) { // modified
                final String template = "%s \"%s\" with different configuration is found on your PC. \nDo you want to override it?";
                final String msg = String.format(template, resource.getDefinition().getTitle(), resource.getName());
                if (!AzureMessager.getMessager().confirm(msg, PROMPT_TITLE)) {
                    return false;
                }
            }
        }
        final List<Connection<?, ?>> existedConnections = profile.getConnections();
        if (CollectionUtils.isNotEmpty(existedConnections)) {
            final Connection<?, ?> existedConnection = existedConnections.stream()
                    .filter(e -> resource.getDefinition().isEnvPrefixSupported() && StringUtils.equals(e.getEnvPrefix(), connection.getEnvPrefix()))
                    .filter(e -> !StringUtils.equals(e.getId(), connection.getId()))
                    .findFirst().orElse(null);
            if (Objects.nonNull(existedConnection)) { // modified
                final Resource<R> connected = (Resource<R>) existedConnection.getResource();
                final String template = "Connection with environment variable prefix \"%s\" is found on your PC, which connect %s \"%s\" to %s \"%s\" \n" +
                        "Do you want to overwrite it?";
                final String msg = String.format(template, connection.getEnvPrefix(),
                        consumer.getDefinition().getTitle(), consumer.getName(),
                        connected.getDefinition().getTitle(), connected.getName());
                final boolean result = AzureMessager.getMessager().confirm(msg, PROMPT_TITLE);
                if (result) {
                    profile.removeConnection(existedConnection);
                }
                return result;
            }
        }
        return true; // is new or not modified.
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
