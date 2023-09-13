/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.common.utils.JdkUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenUtils;
import icons.GradleIcons;
import icons.OpenapiIcons;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AzureArtifact {
    @EqualsAndHashCode.Include
    private final AzureArtifactType type;
    @ToString.Include
    private final String name;
    @EqualsAndHashCode.Include
    private final Object referencedObject;
    private final Project project;

    @Nullable
    public static AzureArtifact createFromFile(@Nullable String path, Project project) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        return Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(path))
            .map(virtualFile -> createFromFile(virtualFile, project)).orElse(null);
    }

    public static AzureArtifact createFromFile(@Nonnull VirtualFile virtualFile, Project project) {
        return new AzureArtifact(AzureArtifactType.File, virtualFile.getName(), virtualFile, project);
    }

    public static AzureArtifact createFromArtifact(@Nonnull Artifact artifact, Project project) {
        return new AzureArtifact(AzureArtifactType.Artifact, artifact.getName(), artifact, project);
    }

    public static AzureArtifact createFromMavenProject(MavenProject mavenProject, Project project) {
        return new AzureArtifact(AzureArtifactType.Maven, mavenProject.getMavenId().getArtifactId(), mavenProject, project);
    }

    public static AzureArtifact createFromGradleProject(ExternalProjectPojo projectPojo, Project project) {
        return new AzureArtifact(AzureArtifactType.Gradle, projectPojo.getName(), projectPojo, project);
    }

    @Nullable
    public String getIdentifier() {
        switch (this.getType()) {
            case Gradle:
                final ExternalProjectPojo pojo = (ExternalProjectPojo) this.getReferencedObject();
                final ExternalProject externalProject = ExternalProjectDataCache.getInstance(project).getRootExternalProject(pojo.getPath());
                return Objects.nonNull(externalProject) ? externalProject.getQName() : null;
            case Maven:
                return this.getReferencedObject().toString();
            case Artifact:
                return ((Artifact) this.getReferencedObject()).getOutputFilePath();
            case File:
                return this.getFileForDeployment();
            default:
                return null;
        }
    }

    @AzureOperation(name = "internal/common.get_artifact_file.artifact", params = {"this.getName()"})
    public String getFileForDeployment() {
        switch (this.getType()) {
            case Gradle:
                return GradleUtils.getTargetFile(project, (ExternalProjectPojo) this.getReferencedObject());
            case Maven:
                return MavenUtils.getSpringBootFinalJarFilePath(project, (MavenProject) this.getReferencedObject());
            case Artifact:
                return ((Artifact) this.getReferencedObject()).getOutputFilePath();
            case File:
                return ((VirtualFile) this.getReferencedObject()).getPath();
            default:
                return null;
        }
    }

    public String getPackaging() {
        switch (this.getType()) {
            case Gradle:
                return FileNameUtils.getExtension(GradleUtils.getTargetFile(project,
                        (ExternalProjectPojo) this.getReferencedObject()));
            case Maven:
                return ((MavenProject) this.getReferencedObject()).getPackaging();
            case Artifact:
                return FileNameUtils.getExtension(((Artifact) this.getReferencedObject()).getOutputFilePath());
            case File:
                return FileNameUtils.getExtension(this.getFileForDeployment());
            default:
                return null;
        }
    }

    public Icon getIcon() {
        switch (type) {
            case Gradle:
                return GradleIcons.Gradle;
            case Maven:
                return OpenapiIcons.RepositoryLibraryLogo;
            case Artifact:
                return ((Artifact) referencedObject).getArtifactType().getIcon();
            case File:
                return AllIcons.FileTypes.Archive;
            default:
                return null;
        }
    }

    @Nullable
    @AzureOperation(name = "boundary/common.get_artifact_module.artifact", params = {"this.getName()"})
    public Module getModule() {
        if (this.getReferencedObject() == null) {
            return null;
        }
        return ApplicationManager.getApplication().runReadAction((Computable<Module>) () -> {
            switch (type) {
                case Gradle:
                    final Path path = Paths.get(((ExternalProjectPojo) referencedObject).getPath());
                    return Optional.ofNullable(VfsUtil.findFile(path, true))
                            .map(f -> ModuleUtil.findModuleForFile(f, this.project)).orElse(null);
                case Maven:
                    return MavenProjectsManager.getInstance(this.project).findModule((MavenProject) referencedObject);
                case Artifact:
                    return Optional.ofNullable(((Artifact) referencedObject).getOutputPath())
                            .map(Path::of).map(FileUtils::getNearestExistingParent)
                            .map(p -> VfsUtil.findFile(p, true))
                            .map(f -> ProjectFileIndex.getInstance(project).getModuleForFile(f)).orElse(null);
                case File:
                    return getNearestParentModuleForFile((VirtualFile) this.getReferencedObject());
                default:
                    return null;
            }
        });
    }

    @Nullable
    private Module getNearestParentModuleForFile(VirtualFile file) {
        Module module = null;
        while (module == null && file != null) {
            module = ModuleUtil.findModuleForFile(file, project);
            file = file.getParent();
        }
        return module;
    }

    @Nullable
    public String getArtifactId() {
        final Object object = this.getReferencedObject();
        switch (this.getType()) {
            case Maven:
                return ((MavenProject) object).getMavenId().getArtifactId();
            case Gradle:
                return getIdentifier();
            default:
                return this.getName();
        }
    }

    public Integer getBytecodeTargetLevel() {
        final Module module = this.getModule();
        Integer level = null;
        if (Objects.nonNull(module)) {
            level = JdkUtils.getTargetBytecodeLanguageLevel(module);
            if (Objects.isNull(level)) {
                level = JdkUtils.getJdkLanguageLevel(module);
            }
        } else if (type == AzureArtifactType.File) {
            level = JdkUtils.getBytecodeLanguageLevel(((VirtualFile) this.getReferencedObject()).toNioPath().toFile());
        }
        if (Objects.nonNull(level)) {
            return level;
        }
        return JdkUtils.getJdkLanguageLevel(this.project);
    }
}
