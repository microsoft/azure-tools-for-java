/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.dependency;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DependencyArtifact {
    private String groupId;
    private String artifactId;
    private String compatibleVersion;
    private String currentVersion;
    private String managementVersion;

    public DependencyArtifact(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public DependencyArtifact(String groupId, String artifactId, String currentVersion) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
    }

    public String getKey() {
        return groupId + ":" + artifactId;
    }
}
