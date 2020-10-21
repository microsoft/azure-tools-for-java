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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.http.LocalFileStorage;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.util.MavenUtils;
import icons.GradleIcons;
import icons.MavenIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.Objects;

public class AzureArtifact {
    private final String name;
    private final AzureArtifactType type;
    private final Object referencedObject;

    private AzureArtifact(final AzureArtifactType type, final String name, Object obj) {
        this.type = type;
        this.name = name;
        this.referencedObject = obj;
    }

    public static AzureArtifact createFromFile(@NotNull String path) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(path);
        return createFromFile(virtualFile);
    }

    public static AzureArtifact createFromFile(@NotNull VirtualFile virtualFile) {
        return new AzureArtifact(AzureArtifactType.File, virtualFile.getName(), virtualFile);
    }

    public static AzureArtifact createFromArtifact(@NotNull Artifact artifact) {
        return new AzureArtifact(AzureArtifactType.Artifact,
                                 artifact.getName(),
                                 artifact);
    }

    public static AzureArtifact createFromMavenProject(MavenProject mavenProject) {
        return new AzureArtifact(AzureArtifactType.Maven,
                                 mavenProject.toString(),
                                 mavenProject);
    }

    public static AzureArtifact createFromGradleProject(ExternalProjectPojo projectPojo) {
        return new AzureArtifact(AzureArtifactType.Gradle, projectPojo.getName(), projectPojo);
    }

    public Icon getIcon() {
        switch (type) {
            case Gradle:
                return GradleIcons.Gradle;
            case Maven:
                return MavenIcons.MavenProject;
            case Artifact:
                return ((Artifact) referencedObject).getArtifactType().getIcon();
            case File:
                return AllIcons.FileTypes.Archive;
            default:
                return null;
        }
    }

    public String getName() {
        return name;
    }

    public AzureArtifactType getType() {
        return type;
    }

    public Object getReferencedObject() {
        return referencedObject;
    }

    @Override
    public String toString() {
        return Objects.toString(name);
    }

    public String getTargetPath() {
        switch (type) {
            case Gradle:
                // it is not required to get a final output jar in getTargetPath
                return Paths.get(((ExternalProjectPojo) referencedObject).getPath(),
                                 String.format("build/lib/%s.jar", getName())).toString();
            case Maven:
                return MavenUtils.getTargetFile((MavenProject) referencedObject);
            case Artifact:
                return ((Artifact) referencedObject).getOutputFilePath();
            case File:
                return ((VirtualFile) referencedObject).getPath();
            default:
                throw new RuntimeException(String.format("Invalid type '%s' for AzureArtifact", type));
        }
    }
}
