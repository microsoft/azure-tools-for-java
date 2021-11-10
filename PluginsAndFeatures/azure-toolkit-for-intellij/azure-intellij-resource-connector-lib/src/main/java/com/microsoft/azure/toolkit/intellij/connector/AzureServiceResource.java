/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AzureServiceResource<T extends IAzureResource<?>> implements Resource<T> {
    @Nonnull
    private final ResourceId id;
    @Getter
    @Nonnull
    private final AzureServiceResource.Definition<T> definition;
    private T data;

    public AzureServiceResource(T data, @Nonnull AzureServiceResource.Definition<T> definition) {
        this(data.id(), definition);
        this.data = data;
    }

    public AzureServiceResource(String id, @Nonnull AzureServiceResource.Definition<T> definition) {
        this.id = ResourceId.fromString(id);
        this.definition = definition;
    }

    public synchronized T getData() {
        if (Objects.isNull(this.data)) {
            this.data = this.definition.getResource(this.id.id());
        }
        return this.data;
    }

    @Override
    public Map<String, String> initEnv(Project project) {
        return this.definition.initEnv(this, project);
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

    @Override
    public void navigate(AnActionEvent event) {
        AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SHOW_PROPERTIES).handle(this.getData(), event);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", this.getDefinition().title, this.getName());
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public abstract static class Definition<T extends IAzureResource<?>> implements ResourceDefinition<T> {
        @EqualsAndHashCode.Include
        private final String name;
        private final String title;
        private final String icon;

        @Override
        public Resource<T> define(T resource) {
            return new AzureServiceResource<>(resource, this);
        }

        public Resource<T> define(String dataId) {
            return new AzureServiceResource<>(dataId, this);
        }

        public abstract T getResource(String dataId);

        @Override
        public boolean write(@Nonnull Element ele, @Nonnull Resource<T> resource) {
            ele.setAttribute(new Attribute("id", resource.getId()));
            ele.addContent(new Element("dataId").addContent(resource.getDataId()));
            return true;
        }

        @Override
        public Resource<T> read(@Nonnull Element ele) {
            final String id = ele.getChildTextTrim("dataId");
            return Optional.ofNullable(id).map(this::define).orElse(null);
        }

        @Override
        public String toString() {
            return this.getTitle();
        }

        public abstract Map<String, String> initEnv(AzureServiceResource<T> data, Project project);
    }
}
