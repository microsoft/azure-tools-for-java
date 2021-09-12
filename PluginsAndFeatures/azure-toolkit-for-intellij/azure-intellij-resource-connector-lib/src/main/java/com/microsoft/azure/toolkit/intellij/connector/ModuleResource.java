/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.lib.Resource;
import com.microsoft.azure.toolkit.intellij.connector.lib.ResourceDefinition;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@RequiredArgsConstructor
public final class ModuleResource implements Resource<String> {
    private final String moduleName;

    @Override
    public ResourceDefinition<String> getDefinition() {
        return Definition.IJ_MODULE;
    }

    @Override
    public String getData() {
        return this.moduleName;
    }

    @Override
    public String getId() {
        return this.moduleName;
    }

    @Override
    public String getName() {
        return this.moduleName;
    }

    @Override
    public String toString() {
        return String.format("Module \"%s\"", this.moduleName);
    }

    @Getter
    @RequiredArgsConstructor
    @Log
    public enum Definition implements ResourceDefinition<String> {
        IJ_MODULE("Jetbrains.IJModule", "Intellij Module");
        private final String name;
        private final String title;
        private final int role = CONSUMER;

        @Override
        public Resource<String> define(String resource) {
            return new ModuleResource(resource);
        }

        @Override
        public AzureFormJPanel<String> getResourcesPanel(@Nonnull String type, final Project project) {
            return new ModulePanel(project);
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
