package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.File;

public class WrappedAzureArtifact implements IArtifact {
    @Getter
    private final AzureArtifact artifact;
    private final Project project;

    public WrappedAzureArtifact(@Nonnull final AzureArtifact artifact, @Nonnull Project project) {
        this.artifact = artifact;
        this.project = project;
    }

    @Override
    public File getFile() {
        final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
        return new File(manager.getFileForDeployment(this.artifact));
    }
}
