/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * the <b>{@code resource}</b> in <b>{@code resource connection}</b><br>
 * it's usually An Azure resource or an intellij module
 */
@Getter
@RequiredArgsConstructor
public class AzureServiceResource<T extends IAzureResource<?>> implements Resource<T> {
    private final T data;
    private final AzureServiceResource.Definition<T> definition;

    @Override
    public String getId() {
        return DigestUtils.md5Hex(this.data.id());
    }

    @Override
    public String getName() {
        return this.data.name();
    }

    @Getter
    @RequiredArgsConstructor
    public static class Definition<T extends IAzureResource<?>> implements ResourceDefinition<T> {
        private final String name;
        private final String title;
        private final String icon;
        @Getter(AccessLevel.NONE)
        private final Supplier<AzureFormJPanel<T>> select;
        private final Function<String, T> get;

        @Override
        public Resource<T> define(T resource) {
            return new AzureServiceResource<>(resource, this);
        }

        @Override
        public AzureFormJPanel<T> getResourcePanel(Project project) {
            return this.select.get();
        }

        @Override
        public boolean write(@Nonnull Element ele, @Nonnull Resource<T> resource) {
            ele.setAttribute(new Attribute("id", resource.getId()));
            ele.addContent(new Element("azureResourceId").addContent(resource.getData().id()));
            return true;
        }

        @Override
        public Resource<T> read(@Nonnull Element ele) {
            final String id = ele.getChildTextTrim("azureResourceId");
            return Optional.ofNullable(this.get.apply(id)).map(this::define).orElse(null);
        }

        @Override
        public String toString() {
            return this.getTitle();
        }
    }
}
