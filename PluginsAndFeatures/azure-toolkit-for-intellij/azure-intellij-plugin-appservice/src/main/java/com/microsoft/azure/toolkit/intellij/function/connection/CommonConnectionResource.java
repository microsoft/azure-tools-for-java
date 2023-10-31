/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.function.connection;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.auth.IntelliJSecureStore;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CommonConnectionResource implements Resource<ConnectionTarget> {

    @Getter
    @Nonnull
    private final CommonConnectionResource.Definition definition;
    @Getter
    @Nonnull
    private ConnectionTarget data;

    public CommonConnectionResource(@Nonnull ConnectionTarget data, @Nonnull CommonConnectionResource.Definition definition) {
        this.data = data;
        this.definition = definition;
    }

    @Nonnull
    @Override
    public ResourceDefinition<ConnectionTarget> getDefinition() {
        return this.definition;
    }

    @Override
    public ConnectionTarget getData() {
        return this.data;
    }

    @Override
    @EqualsAndHashCode.Include
    public String getDataId() {
        return getData().getId();
    }

    @Override
    public String getName() {
        return getData().getName();
    }

    @Override
    public Map<String, String> initEnv(Project project) {
        final ConnectionTarget connection = getData();
        return Collections.singletonMap(connection.getName(), connection.getConnectionString());
    }

    @Override
    public String toString() {
        return this.data.getName();
    }

    public String getEnvPrefix() {
        return this.data.getName();
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    public static class Definition implements ResourceDefinition<ConnectionTarget>, FunctionSupported<ConnectionTarget> {
        public static final Definition INSTANCE = new Definition();

        @Override
        @EqualsAndHashCode.Include
        public String getName() {
            return "Common Connection (Connection String)";
        }

        @Override
        public Resource<ConnectionTarget> define(ConnectionTarget resource) {
            return new CommonConnectionResource(resource, this);
        }

        @Override
        public AzureFormJPanel<Resource<ConnectionTarget>> getResourcePanel(Project project) {
            return new CommonConnectionCreationPanel();
        }

        @Override
        public List<Resource<ConnectionTarget>> getResources(Project project) {
            return Collections.emptyList();
        }

        @Override
        public boolean write(@Nonnull Element element, @Nonnull Resource<ConnectionTarget> resource) {
            final ConnectionTarget target = resource.getData();
            element.setAttribute(new Attribute("id", resource.getId()));
            element.addContent(new Element("resourceId").addContent(resource.getDataId()));
            element.addContent(new Element("name").addContent(target.getName()));
            IntelliJSecureStore.getInstance().savePassword(Definition.class.getName(), resource.getDataId(), null, target.getConnectionString());
            return true;
        }

        @Override
        public Resource<ConnectionTarget> read(@Nonnull Element element) {
            final String id = Optional.ofNullable(element.getChildTextTrim("resourceId")).orElseGet(() -> element.getChildTextTrim("dataId"));
            final String name = element.getChildTextTrim("name");
            final String triggerType = element.getChildTextTrim("triggerType");
            final String connectionString = IntelliJSecureStore.getInstance().loadPassword(Definition.class.getName(), id, null);
            final ConnectionTarget target = Objects.isNull(id) ? null :
                ConnectionTarget.builder().id(id).name(name).connectionString(connectionString).build();
            return Optional.ofNullable(target).map(this::define).orElse(null);
        }

        @Nullable
        @Override
        public String getIcon() {
            return AzureIcons.Connector.CONNECT.getIconPath();
        }

        @Override
        public String toString() {
            return this.getTitle();
        }

        @Nonnull
        @Override
        public String getResourceType() {
            return "common";
        }

        @Override
        public Map<String, String> getPropertiesForFunction(@Nonnull ConnectionTarget resource, @Nonnull Connection connection) {
            return Collections.singletonMap(resource.getName(), getResourceConnectionString(resource));
        }

        @Nullable
        @Override
        public String getResourceConnectionString(@Nonnull ConnectionTarget resource) {
            return resource.getConnectionString();
        }

        @Override
        public boolean isEnvPrefixSupported() {
            return false;
        }
    }
}
