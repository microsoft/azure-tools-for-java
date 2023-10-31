/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ModuleResource implements Resource<String> {
    @EqualsAndHashCode.Include
    private final String moduleName;
    private final ResourceDefinition<String> definition = Definition.IJ_MODULE;

    @Override
    public String getData() {
        return this.moduleName;
    }

    @Override
    public String getId() {
        return this.moduleName;
    }

    @Override
    public String getDataId() {
        return this.moduleName;
    }

    @Override
    public String getName() {
        return this.moduleName;
    }

    @Getter
    @RequiredArgsConstructor
    @EqualsAndHashCode
    public static class Definition implements ResourceDefinition<String> {
        public static final Definition IJ_MODULE = new Definition();
        private final String name;
        private final String title;
        private final String icon;
        private final int role = CONSUMER;

        public Definition() {
            this.name = "Jetbrains.IJModule";
            this.title = "Intellij Module";
            this.icon = "/icons/module";
        }

        @Override
        public Resource<String> define(String resource) {
            return new ModuleResource(resource);
        }

        @Override
        public AzureFormJPanel<Resource<String>> getResourcePanel(final Project project) {
            return new ModulePanel(project);
        }

        @Override
        public List<Resource<String>> getResources(Project project) {
            return Arrays.stream(ModuleManager.getInstance(project).getModules()).map(m -> this.define(m.getName())).toList();
        }

        @Override
        public boolean write(@Nonnull Element resourceEle, @Nonnull Resource<String> resource) {
            return false;
        }

        @Override
        @Nullable
        public ModuleResource read(@Nonnull Element resourceEle) {
            throw new AzureToolkitRuntimeException("loading a persisted module resource is not allowed");
        }

        @Override
        public String toString() {
            return this.getTitle();
        }
    }
}
