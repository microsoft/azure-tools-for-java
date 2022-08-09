/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model.module;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.util.MavenUtils;
import icons.MavenIcons;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MavenProjectModule implements ProjectModule {
    private final Project project;
    private final MavenProject mavenProject;

    public MavenProjectModule(@Nonnull final Project project, @Nonnull final MavenProject mavenProject) {
        this.project = project;
        this.mavenProject = mavenProject;
    }

    public static List<MavenProjectModule> listMavenModules(@Nonnull Project project) {
        return MavenUtils.getMavenProjects(project).stream().map(mavenProject -> new MavenProjectModule(project, mavenProject)).collect(Collectors.toList());
    }

    public MavenArtifact getMavenDependency(final String groupId, final String artifactId) {
        return mavenProject.getDependencies().stream()
                .filter(dependency -> StringUtils.equalsIgnoreCase(groupId, dependency.getGroupId()) &&
                        StringUtils.equalsIgnoreCase(artifactId, dependency.getArtifactId())).findFirst().orElse(null);
    }

    @Override
    public String getName() {
        return mavenProject.getDisplayName();
    }

    @Override
    public Icon getIcon() {
        return MavenIcons.MavenProject;
    }
}
