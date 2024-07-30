/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AzureServiceResource<T extends AzResource> implements Resource<T> {
    public static final String SUBSCRIPTION_ID_KEY = String.format("%s_SUBSCRIPTION_ID", Connection.ENV_PREFIX);
    public static final String RESOURCE_GROUP_KEY = String.format("%s_RESOURCE_GROUP", Connection.ENV_PREFIX);
    @Nonnull
    private final ResourceId azId;
    @Getter
    @Nonnull
    private final AzureServiceResource.Definition<T> definition;
    @Nonnull
    @Getter
    private final String id;
    private final T data;
    @Getter
    @Setter
    private String connectionId;

    public AzureServiceResource(@Nonnull T data, @Nonnull AzureServiceResource.Definition<T> definition) {
        this(data, null, definition);
    }

    public AzureServiceResource(@Nonnull T data, @Nullable final String id, @Nonnull AzureServiceResource.Definition<T> definition) {
        this.azId = ResourceId.fromString(data.getId());
        this.id = StringUtils.isBlank(id) ? DigestUtils.md5Hex(this.azId.id()) : id;
        this.definition = definition;
        this.data = data;
    }

    @Deprecated
    public AzureServiceResource(@Nonnull String dataId, @Nonnull AzureServiceResource.Definition<T> definition) {
        this(dataId, null, definition);
    }

    public AzureServiceResource(@Nonnull final String dataId, @Nullable final String id, @Nonnull AzureServiceResource.Definition<T> definition) {
        this.azId = ResourceId.fromString(dataId);
        this.id = StringUtils.isBlank(id) ? DigestUtils.md5Hex(this.azId.id()) : id;
        this.definition = definition;
        this.data = null;
    }

    @Nullable
    public T getData() {
        if (AbstractAzResourceModule.isMocked(this.azId.id()) || Azure.az(IAzureAccount.class).isLoggedIn()) {
            return Optional.ofNullable(this.data).orElseGet(() -> this.definition.getResource(this.azId.id(), this.getId()));
        }
        return null;
    }

    @Override
    public Map<String, String> initEnv(Project project, Connection<?,?> connection) {
        final T resource = this.getData();
        if (resource == null || !resource.exists()) {
            throw new AzureToolkitRuntimeException(String.format("%s '%s' does not exist.", this.getResourceType(), this.getName()));
        }
        final Map<String, String> properties = connection.isManagedIdentityConnection() && definition instanceof IManagedIdentitySupported identitySupported ?
                identitySupported.initIdentityEnv(connection, project) : this.definition.initEnv(this, project);
        final Map<String, String> result = new HashMap<>(properties);
        if (!(resource instanceof AbstractConnectionStringAzResource<?>)) {
            result.put(SUBSCRIPTION_ID_KEY, resource.getSubscriptionId());
            result.put(RESOURCE_GROUP_KEY, resource.getResourceGroupName());
        }
        return result;
    }

    @Override
    @EqualsAndHashCode.Include
    public String getDataId() {
        return this.azId.id();
    }

    @Override
    public String getName() {
        return this.azId.name();
    }

    public String getResourceType() {
        final AbstractAzResource<?, ?, ?> parent = Optional.ofNullable(this.azId.parent())
            .map(parentId -> Azure.az().getById(parentId.id())).orElse(null);
        return Objects.isNull(parent) ? azId.resourceType() :
            parent.getSubModules().stream()
                .filter(module -> StringUtils.equals(module.getName(), this.azId.resourceType()))
                .findFirst()
                .map(AzResourceModule::getResourceTypeName)
                .orElseGet(azId::resourceType);
    }

    @Override
    public void navigate(AnActionEvent event) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SHOW_PROPERTIES).handle(this.getData(), event);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getDefinition().title, this.getName());
    }

    @Override
    public boolean isValidResource() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return true;
        }
        try {
            return Optional.ofNullable(getData()).map(AzResource::getFormalStatus).map(AzResource.FormalStatus::isConnected).orElse(false);
        } catch (final Throwable e) {
            return false;
        }
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public abstract static class Definition<T extends AzResource> implements ResourceDefinition<T> {
        @EqualsAndHashCode.Include
        private final String name;
        private final String title;
        private final String icon;

        @Override
        public Resource<T> define(T resource, String id) {
            return new AzureServiceResource<>(resource, id, this);
        }

        public Resource<T> define(@Nonnull String dataId, @Nullable String id) {
            return new AzureServiceResource<>(dataId, id, this);
        }

        @Nullable
        public T getResource(@Nonnull String dataId) {
            return this.getResource(dataId, null);
        }

        @Nullable
        public abstract T getResource(@Nonnull String dataId, @Nullable final String id);

        @Override
        public boolean write(@Nonnull Element ele, @Nonnull Resource<T> resource) {
            ele.setAttribute(new Attribute("id", resource.getId()));
            ele.addContent(new Element("resourceId").addContent(resource.getDataId()));
            return true;
        }

        @Override
        @Nullable
        public Resource<T> read(@Nonnull Element ele) {
            final String id = ele.getAttributeValue("id");
            final String dataId = Optional.ofNullable(ele.getChildTextTrim("resourceId")).orElseGet(() -> ele.getChildTextTrim("dataId"));
            return StringUtils.isNoneBlank(dataId, id) ? this.define(dataId, id) : null;
        }

        @Override
        public String toString() {
            return this.getTitle();
        }

        public abstract Map<String, String> initEnv(AzureServiceResource<T> data, Project project);

        public List<String> getEnvironmentVariablesKey() {
            return Arrays.asList(SUBSCRIPTION_ID_KEY, RESOURCE_GROUP_KEY);
        }
    }
}
