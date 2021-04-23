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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class ModuleResource implements Resource {
    public static final String TYPE = Definition.IJ_MODULE.type;
    private final String type = Definition.IJ_MODULE.type;
    @EqualsAndHashCode.Include
    private final String moduleName;
    private Module module;

    @Override
    public String getId() {
        return moduleName;
    }

    public Module getModule() {
        if (this.module == null) {
            final Project project = ProjectManager.getInstance().getOpenProjects()[0];
            this.module = Arrays.stream(ModuleManager.getInstance(project).getModules())
                    .filter(m -> Objects.equals(m.getName(), moduleName)).findAny()
                    .orElse(null);
        }
        return this.module;
    }

    public String toString() {
        return String.format("Module \"%s\"", this.moduleName);
    }

    @Getter
    @RequiredArgsConstructor
    @Log
    public enum Definition implements ResourceDefinition<ModuleResource> {
        IJ_MODULE("Jetbrains.IJModule", "Intellij Module");
        private final String type;
        private final String title;
        private final int role = CONSUMER;

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
            return this.getTitle();
        }
    }
}
