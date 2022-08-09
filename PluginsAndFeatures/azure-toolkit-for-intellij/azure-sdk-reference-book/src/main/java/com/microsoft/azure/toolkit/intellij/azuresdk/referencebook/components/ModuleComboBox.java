/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.GradleProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.MavenProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.ProjectModule;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleComboBox extends AzureComboBox<ProjectModule> {
    private final Project project;

    public ModuleComboBox() {
        this(ProjectManager.getInstance().getDefaultProject());
    }

    public ModuleComboBox(Project project) {
        super();
        this.project = project;
    }

    @Nonnull
    @Override
    protected List<ProjectModule> loadItems() {
        final List<GradleProjectModule> gradleProjectModules = GradleProjectModule.listGradleModules(project);
        final List<MavenProjectModule> mavenProjectModules = MavenProjectModule.listMavenModules(project);
        return Stream.of(mavenProjectModules, gradleProjectModules).flatMap(List::stream)
                .sorted((Comparator<ProjectModule>) (first, second) -> StringUtils.compare(first.getName(), second.getName()))
                .collect(Collectors.toList());
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof ProjectModule ? ((ProjectModule) item).getName() : super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        return item instanceof ProjectModule ? ((ProjectModule) item).getIcon() : super.getItemIcon(item);
    }
}
