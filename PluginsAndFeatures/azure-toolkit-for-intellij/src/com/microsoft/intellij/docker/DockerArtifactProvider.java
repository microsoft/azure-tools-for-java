package com.microsoft.intellij.docker;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

public abstract class DockerArtifactProvider {
    public static final ExtensionPointName<DockerArtifactProvider> EXTENSION_POINT_NAME =
            ExtensionPointName.create("com.microsoft.intellij.dockerArtifactProvider");

    public abstract Collection<File> getPaths(@NotNull Project project);
}
