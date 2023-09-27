/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.ListItemDescriptorAdapter;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.GradleProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.MavenProjectModule;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.module.ProjectModule;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ModuleDependencyComboBox extends AzureComboBox<ProjectModule> {
    private static final String MODULES_WITHOUT_DEPENDENCIES = "Modules without dependencies";
    private static final String MODULES_WITH_OUTDATED_DEPENDENCIES = "Modules with outdated dependencies";
    private static final String MODULES_WITH_DEPENDENCIES = "Modules with dependencies";
    private static final List<String> GROUPS = Arrays.asList(MODULES_WITHOUT_DEPENDENCIES, MODULES_WITH_OUTDATED_DEPENDENCIES, MODULES_WITH_DEPENDENCIES);
    private final Project project;
    private final ModuleDependencyItemDescriptor render = new ModuleDependencyItemDescriptor();

    private AzureSdkArtifactEntity entity;
    private String version;
    private Map<String, ? extends List<? extends ProjectModule>> moduleSeparatorModelMap = new HashMap<>();

    public ModuleDependencyComboBox() {
        this(ProjectManager.getInstance().getDefaultProject());
    }

    public ModuleDependencyComboBox(Project project) {
        super();
        this.project = project;
        this.setRenderer(new GroupedItemsListRenderer<>(new ModuleDependencyItemDescriptor()));
    }

    public void setArtifact(final AzureSdkArtifactEntity pkg, final String version) {
        this.entity = pkg;
        this.version = version;
        this.reloadItems();
    }

    @Nullable
    @Override
    protected ProjectModule doGetDefaultValue() {
        return CacheManager.getUsageHistory(ProjectModule.class)
                .peek(v -> Objects.isNull(project) || Objects.equals(project, v.getProject()));
    }

    @Nonnull
    @Override
    protected List<ProjectModule> loadItems() {
        final List<GradleProjectModule> gradleProjectModules = GradleProjectModule.listGradleModules(project);
        final List<MavenProjectModule> mavenProjectModules = MavenProjectModule.listMavenModules(project);
        this.moduleSeparatorModelMap = Stream.of(mavenProjectModules, gradleProjectModules)
                .flatMap(List::stream)
                .sorted(Comparator.comparing(ProjectModule::getName))
                .collect(Collectors.groupingBy(m -> getModuleSeparatorModel(m), Collectors.mapping(e -> e, Collectors.toList())));
        return moduleSeparatorModelMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> GROUPS.indexOf(entry.getKey())))
                .flatMap(entry -> entry.getValue().stream()).collect(Collectors.toList());
    }

    private String getModuleSeparatorModel(final ProjectModule module) {
        return !module.isDependencyExists(entity) ? MODULES_WITHOUT_DEPENDENCIES :
                (module.isDependencyUpToDate(entity, version) ? MODULES_WITH_DEPENDENCIES : MODULES_WITH_OUTDATED_DEPENDENCIES);
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
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

    class ModuleDependencyItemDescriptor extends ListItemDescriptorAdapter<ProjectModule> {
        @Override
        public String getTextFor(ProjectModule value) {
            return value.getName();
        }

        @Override
        public String getCaptionAboveOf(ProjectModule value) {
            return getModuleSeparatorModel(value);
        }

        @Override
        public Icon getIconFor(ProjectModule value) {
            return value.getIcon();
        }

        @Override
        public boolean hasSeparatorAboveOf(ProjectModule value) {
            return Optional.of(getModuleSeparatorModel(value))
                    .map(moduleSeparatorModelMap::get)
                    .map(list -> list.get(0) == value).orElse(false); // only show separator for first element of each group
        }
    }
}
