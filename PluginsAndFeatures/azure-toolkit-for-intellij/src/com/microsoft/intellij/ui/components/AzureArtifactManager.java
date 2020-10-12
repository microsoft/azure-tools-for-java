/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.ui.components;

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.MavenUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;

import java.util.*;
import java.util.stream.Collectors;

public class AzureArtifactManager {
    private static Map<Project, AzureArtifactManager> projectAzureArtifactManagerMap = new HashMap<>();
    private final Project project;

    private AzureArtifactManager(Project project) {
        this.project = project;
    }

    public static AzureArtifactManager getInstance(@NotNull Project project) {
        return projectAzureArtifactManagerMap.computeIfAbsent(project, key ->
                                                                      new AzureArtifactManager(project)
                                                             );
    }

    public List<AzureArtifact> getAllSupportedAzureArtifacts() {
        List<AzureArtifact> azureArtifacts = new ArrayList<>();
        List<ExternalProjectPojo> gradleProjects = GradleUtils.listGradleProjects(project);
        if (Objects.nonNull(gradleProjects)) {
            azureArtifacts.addAll(gradleProjects.stream()
                                                .map(AzureArtifact::createFromGradleProject)
                                                .collect(Collectors.toList()));
        }
        List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
        if (Objects.nonNull(mavenProjects)) {
            azureArtifacts.addAll(
                    mavenProjects.stream().map(AzureArtifact::createFromMavenProject).collect(Collectors.toList()));
        }
        List<Artifact> artifactList = MavenRunTaskUtil.collectProjectArtifact(project);
        if (Objects.nonNull(artifactList)) {
            azureArtifacts.addAll(
                    artifactList.stream().map(AzureArtifact::createFromArtifact).collect(Collectors.toList()));
        }
        return azureArtifacts;
    }

    public String getArtifactIdentifier(AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Gradle:
                return getGradleProjectId((ExternalProjectPojo) artifact.getReferencedObject());
            case Maven:
                return artifact.getReferencedObject().toString();
            case Artifact:
                return ((Artifact) artifact.getReferencedObject()).getOutputFilePath();
            default:
                return null;
        }
    }

    public String getFileForDeployment(AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Gradle:
                return GradleUtils.getTargetFile(project, (ExternalProjectPojo) artifact.getReferencedObject());
            case Maven:
                return MavenUtils.getSpringBootFinalJarFilePath(project, (MavenProject) artifact.getReferencedObject());
            case Artifact:
                return ((Artifact) artifact.getReferencedObject()).getOutputFilePath();
            default:
                return null;
        }
    }

    public AzureArtifact getAzureArtifactById(String artifactId) {
        return getAllSupportedAzureArtifacts().stream().filter(artifact -> StringUtils.equals(getArtifactIdentifier(
                artifact), artifactId)).findFirst().orElse(null);
    }

    private String getGradleProjectId(ExternalProjectPojo gradleProjectPojo) {
        ExternalProject externalProject =
                ExternalProjectDataCache.getInstance(project).getRootExternalProject(gradleProjectPojo.getPath());
        return Objects.nonNull(externalProject) ? externalProject.getQName() : null;
    }
}
