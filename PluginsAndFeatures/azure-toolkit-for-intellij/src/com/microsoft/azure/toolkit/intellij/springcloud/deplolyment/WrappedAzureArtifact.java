package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.File;

public class WrappedAzureArtifact implements IArtifact {
    @Getter
    @Nullable
    private final AzureArtifact artifact;

    public WrappedAzureArtifact(@Nullable final AzureArtifact artifact) {
        this.artifact = artifact;
    }

    @Override
    public File getFile() {
        return null;
    }
}
