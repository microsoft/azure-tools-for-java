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
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
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
    private final ResourceId id;
    @Getter
    @Nonnull
    private final AzureServiceResource.Definition<T> definition;
    @Getter
    @Setter
    private String connectionId;

    public AzureServiceResource(@Nonnull T data, @Nonnull AzureServiceResource.Definition<T> definition) {
        this(data.getId(), definition);
    }

    @Deprecated
    public AzureServiceResource(@Nonnull String id, @Nonnull AzureServiceResource.Definition<T> definition) {
        this.id = ResourceId.fromString(id);
        this.definition = definition;
    }

    public T getData() {
        return this.definition.getResource(this.id.id());
    }

    @Override
    public Map<String, String> initEnv(Project project) {
        final T resource = this.getData();
        if (resource == null || !resource.exists()) {
            throw new AzureToolkitRuntimeException(String.format("%s '%s' does not exist.", this.getResourceType(), this.getName()));
        }
        final Map<String, String> result = new HashMap<>();
        result.putAll(this.definition.initEnv(this, project));
        result.put(SUBSCRIPTION_ID_KEY, resource.getSubscriptionId());
        result.put(RESOURCE_GROUP_KEY, resource.getResourceGroupName());
        return result;
    }

    @Override
    @EqualsAndHashCode.Include
    public String getDataId() {
        return this.id.id();
    }

    @Override
    public String getName() {
        return this.id.name();
    }

    public String getResourceType() {
        final AbstractAzResource<?, ?, ?> parent = Optional.ofNullable(this.id.parent())
                .map(parentId -> Azure.az().getById(parentId.id())).orElse(null);
        return Objects.isNull(parent) ? id.resourceType() :
                parent.getSubModules().stream()
                        .filter(module -> StringUtils.equals(module.getName(), this.id.resourceType()))
                        .findFirst()
                        .map(AzResourceModule::getResourceTypeName)
                        .orElseGet(id::resourceType);
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
        public Resource<T> define(T resource) {
            return new AzureServiceResource<>(resource, this);
        }

        @Deprecated
        public Resource<T> define(String dataId) {
            return new AzureServiceResource<>(dataId, this);
        }

        public abstract T getResource(String dataId);

        @Override
        public boolean write(@Nonnull Element ele, @Nonnull Resource<T> resource) {
            ele.setAttribute(new Attribute("id", resource.getId()));
            ele.addContent(new Element("resourceId").addContent(resource.getDataId()));
            return true;
        }

        @Override
        public Resource<T> read(@Nonnull Element ele) {
            final String id = Optional.ofNullable(ele.getChildTextTrim("resourceId")).orElseGet(() -> ele.getChildTextTrim("dataId"));
            return Optional.ofNullable(id).map(this::define).orElse(null);
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
