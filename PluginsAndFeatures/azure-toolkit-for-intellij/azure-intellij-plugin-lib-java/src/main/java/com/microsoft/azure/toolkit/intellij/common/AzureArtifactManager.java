/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureArtifactType.File;

public class AzureArtifactManager {
    private static final Map<Project, AzureArtifactManager> projectAzureArtifactManagerMap = new HashMap<>();
    private final Project project;

    private AzureArtifactManager(Project project) {
        this.project = project;
    }

    public static AzureArtifactManager getInstance(@NotNull Project project) {
        return projectAzureArtifactManagerMap.computeIfAbsent(project, key -> new AzureArtifactManager(project));
    }

    public List<AzureArtifact> getAllSupportedAzureArtifacts() {
        return prepareAzureArtifacts(null);
    }

    public List<AzureArtifact> getSupportedAzureArtifactsForSpringCloud() {
        return prepareAzureArtifacts(packaging -> StringUtils.equals(packaging, MavenConstants.TYPE_JAR));
    }

    @Nullable
    public AzureArtifact getAzureArtifactById(String artifactId) {
        return getAllSupportedAzureArtifacts().stream()
            .filter(artifact -> StringUtils.equals(artifact.getIdentifier(), artifactId))
            .findFirst().orElse(null);
    }

    @Nullable
    public AzureArtifact getAzureArtifactById(AzureArtifactType azureArtifactType, String artifactId) {
        return azureArtifactType == File ? AzureArtifact.createFromFile(artifactId, project) : getAzureArtifactById(artifactId);
    }

    public boolean equalsAzureArtifact(AzureArtifact artifact1, AzureArtifact artifact2) {
        if (Objects.isNull(artifact1) || Objects.isNull(artifact2)) {
            return artifact1 == artifact2;
        }
        if (artifact1.getType() != artifact2.getType()) {
            // Artifact with different type may have same identifier, for instance, File and Artifact, both of them use file path as identifier
            // todo: re-design identifier, make it include the artifact type info
            return false;
        }
        return StringUtils.equals(artifact1.getIdentifier(), artifact2.getIdentifier());
    }

    private List<AzureArtifact> prepareAzureArtifacts(Predicate<String> packagingFilter) {
        final List<ExternalProjectPojo> gradleProjects = GradleUtils.listGradleRootProjectPojo(project);
        final List<AzureArtifact> azureArtifacts = new ArrayList<>(gradleProjects.stream()
            .map(p -> AzureArtifact.createFromGradleProject(p, project))
            .collect(Collectors.toList()));
        final List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
        azureArtifacts.addAll(mavenProjects.stream().map(p -> AzureArtifact.createFromMavenProject(p, project)).collect(Collectors.toList()));

        final List<Artifact> artifactList = MavenRunTaskUtil.collectProjectArtifact(project);
        azureArtifacts.addAll(artifactList.stream().map(p -> AzureArtifact.createFromArtifact(p, project)).collect(Collectors.toList()));

        if (packagingFilter == null) {
            return azureArtifacts;
        }
        return azureArtifacts.stream().filter(artifact -> packagingFilter.test(artifact.getPackaging())).collect(Collectors.toList());
    }
}
