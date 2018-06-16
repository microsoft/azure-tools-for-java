package com.microsoft.intellij.docker;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class DockerArtifactsFromJavaProjectModel extends DockerArtifactProvider {
    @Override
    public Collection<File> getPaths(@NotNull Project project) {
        final ArrayList<File> result = new ArrayList<>();

        for (Artifact item : ArtifactUtil.getArtifactWithOutputPaths(project)) {
            String path = item.getOutputFilePath();
            if (path != null && (path.toLowerCase().endsWith(".war") || path.toLowerCase().endsWith(".jar")) &&
                    AzureDockerValidationUtils.validateDockerArtifactPath(path)) {
                result.add(new File(path));
            }
        }

        return result;
    }
}
