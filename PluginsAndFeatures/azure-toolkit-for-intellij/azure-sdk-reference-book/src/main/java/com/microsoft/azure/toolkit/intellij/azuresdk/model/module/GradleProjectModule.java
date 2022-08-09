/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model.module;

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.project.Project;
import com.microsoft.intellij.util.GradleUtils;
import icons.GradleIcons;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.gradle.model.ExternalDependency;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.model.UnresolvedExternalDependency;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class GradleProjectModule implements ProjectModule {
    private final Project project;
    private final ExternalProject externalProject;

    public GradleProjectModule(@Nonnull final Project project, @Nonnull final ExternalProject externalProject) {
        this.project = project;
        this.externalProject = externalProject;
    }

    public static List<GradleProjectModule> listGradleModules(@Nonnull Project project) {
        return GradleUtils.listGradleProjects(project).stream().map(pojo -> new GradleProjectModule(project, pojo)).collect(Collectors.toList());
    }

    public ExternalDependency getGradleDependency(final String groupId, final String artifactId) {
        final ExternalSourceSet main = externalProject.getSourceSets().get("main");
        final Collection<ExternalDependency> externalDependencies = Optional.ofNullable(main).map(ExternalSourceSet::getDependencies).orElse(Collections.emptyList());
        return externalDependencies.stream()
                .filter(dependency -> StringUtils.equalsIgnoreCase(groupId, dependency.getGroup()) &&
                        StringUtils.equalsIgnoreCase(artifactId, dependency.getName()))
                .filter(dependency -> !(dependency instanceof UnresolvedExternalDependency)).findFirst().orElse(null);
    }

    @Override
    public String getName() {
        return externalProject.getName();
    }

    @Override
    public Icon getIcon() {
        return GradleIcons.Gradle;
    }
}
