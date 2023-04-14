/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.container.model;

import com.github.dockerjava.api.model.Image;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DockerImage {
    @Builder.Default
    @EqualsAndHashCode.Include
    private boolean isDraft = true;
    @EqualsAndHashCode.Include
    private String repositoryName;
    @EqualsAndHashCode.Include
    private String tagName;
    @EqualsAndHashCode.Include
    private File dockerFile;
    @EqualsAndHashCode.Include
    private String imageId;
    private File baseDirectory;
    // todo: check whether we need to add artifact as a field of image
    private AzureArtifact azureArtifact;

    public DockerImage(@Nonnull final Image image) {
        this.isDraft = false;
        this.imageId = image.getId();
        final String[] repoTags = image.getRepoTags();
        if (!ArrayUtils.isEmpty(repoTags)) {
            final String[] split = StringUtils.split(repoTags[0], ":");
            this.repositoryName = ArrayUtils.isEmpty(split) ? null : split[0];
            this.tagName = ArrayUtils.getLength(split) < 2 ? null : split[1];
        }
    }

    public DockerImage(@Nonnull final VirtualFile virtualFile) {
        this.isDraft = true;
        this.dockerFile = new File(virtualFile.getPath());
        this.baseDirectory = this.dockerFile.getParentFile();
        this.repositoryName = "image";
        this.tagName = "latest";
    }

    public String getImageName() {
        return String.format("%s:%s", this.repositoryName, this.tagName);
    }
}
