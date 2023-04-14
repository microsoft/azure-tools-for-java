/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.buildimage;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;

import javax.annotation.Nullable;

public interface IDockerConfiguration {
    Project getProject();
    @Nullable
    DockerImage getDockerImageConfiguration();

    @Nullable
    DockerHost getDockerHostConfiguration();

    @Nullable
    default String getRegistryUrl() {
        return null;
    }
}
