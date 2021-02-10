/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

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

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getCompatibleVersion() {
        return compatibleVersion;
    }

    public void setCompatibleVersion(String compatibleVersion) {
        this.compatibleVersion = compatibleVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getKey() {
        return groupId + ":" + artifactId;
    }

    public void setManagementVersion(final String managementVersion) {
        this.managementVersion = managementVersion;
    }

    public String getManagementVersion() {
        return managementVersion;
    }
}
