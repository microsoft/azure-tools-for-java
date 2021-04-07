/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.jdom.Element;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ModuleResource implements Resource {
    private final Definition definition = Definition.IJ_MODULE;
    private final String moduleName;

    @Override
    @EqualsAndHashCode.Include
    public String getBizId() {
        return moduleName;
    }

    @Override
    public String getId() {
        return moduleName;
    }

    public Module getModule() {
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
        return Arrays.stream(ModuleManager.getInstance(project).getModules())
                .filter(m -> Objects.equals(m.getName(), moduleName)).findAny()
                .orElse(null);
    }

    @Getter
    @RequiredArgsConstructor
    @Log
    public enum Definition implements ResourceDefinition<ModuleResource> {
        IJ_MODULE("Jetbrains.IJModule", "Intellij Module");
        private final String type;
        private final String name;

        @Override
        public int isResourceOrConsumer() {
            return CONSUMER;
        }

        @Override
        public AzureFormJPanel<ModuleResource> getResourcesPanel(String type, final Project project) {
            return new ModulePanel(project);
        }

        @Override
        public boolean write(Element resourceEle, ModuleResource resource) {
            return false;
        }

        @Nullable
        public ModuleResource read(Element resourceEle) {
            throw new AzureToolkitRuntimeException("loading a persisted module resource is not allowed");
        }

        public String toString() {
            return this.getName();
        }
    }
}
