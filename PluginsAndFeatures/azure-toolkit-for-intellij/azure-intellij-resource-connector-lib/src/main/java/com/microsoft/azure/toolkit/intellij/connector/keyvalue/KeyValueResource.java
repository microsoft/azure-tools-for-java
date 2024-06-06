/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.keyvalue;

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
import org.apache.commons.codec.digest.DigestUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class KeyValueResource implements Resource<KeyValueData> {
    @Nonnull
    private final KeyValueData data;
    @Nonnull
    private final KeyValueResource.Definition definition;

    @Override
    public String getId() {
        return DigestUtils.md5Hex(this.getDataId());
    }

    @Override
    @EqualsAndHashCode.Include
    public String getDataId() {
        return getData().getId();
    }

    @Override
    public String getName() {
        return getData().getKey();
    }

    @Override
    public Map<String, String> initEnv(Project project) {
        final KeyValueData connection = getData();
        return Collections.singletonMap(connection.getKey(), connection.getValue());
    }

    @Override
    public String toString() {
        return this.data.getKey();
    }

    public String getEnvPrefix() {
        return this.data.getKey();
    }

    @Getter
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @RequiredArgsConstructor
    public static class Definition implements ResourceDefinition<KeyValueData>, FunctionSupported<KeyValueData> {
        public static final Definition INSTANCE = new Definition();

        @Override
        @EqualsAndHashCode.Include
        public String getName() {
            return "Common Connection (Key/Value)";
        }

        @Override
        public Resource<KeyValueData> define(KeyValueData resource, String id) {
            return new KeyValueResource(resource, this);
        }

        @Override
        public AzureFormJPanel<Resource<KeyValueData>> getResourcePanel(Project project) {
            return new KeyValueConnectorPanel();
        }

        @Override
        public List<Resource<KeyValueData>> getResources(Project project) {
            return Collections.emptyList();
        }

        @Override
        public boolean write(@Nonnull Element element, @Nonnull Resource<KeyValueData> resource) {
            final KeyValueData target = resource.getData();
            element.setAttribute(new Attribute("id", resource.getId()));
            element.addContent(new Element("resourceId").addContent(resource.getDataId()));
            element.addContent(new Element("name").addContent(target.getKey()));
            IntelliJSecureStore.getInstance().savePassword(Definition.class.getName(), resource.getDataId(), null, target.getValue());
            return true;
        }

        @Override
        public Resource<KeyValueData> read(@Nonnull Element element) {
            final String id = Optional.ofNullable(element.getChildTextTrim("resourceId")).orElseGet(() -> element.getChildTextTrim("dataId"));
            final String name = element.getChildTextTrim("name");
            final String connectionString = IntelliJSecureStore.getInstance().loadPassword(Definition.class.getName(), id, null);
            final KeyValueData target = Objects.isNull(id) ? null :
                KeyValueData.builder().id(id).key(name).value(connectionString).build();
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
        public Map<String, String> getPropertiesForFunction(@Nonnull KeyValueData resource, @Nonnull Connection connection) {
            return Collections.singletonMap(resource.getKey(), getResourceConnectionString(resource));
        }

        @Nullable
        @Override
        public String getResourceConnectionString(@Nonnull KeyValueData resource) {
            return resource.getValue();
        }

        @Override
        public boolean isEnvPrefixSupported() {
            return false;
        }
    }
}
