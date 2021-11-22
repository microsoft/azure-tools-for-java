/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.wizard.model;

public class FunctionArtifactModel {
    private String groupId;
    private String artifactId;
    private String version;
    private String packageName;

    public static FunctionArtifactModel getDefaultFunctionProjectConfig() {
        FunctionArtifactModel model = new FunctionArtifactModel();
        model.setArtifactId("FirstProject");
        model.setGroupId("com.example");
        model.setPackageName("com.example");
        model.setVersion("0.1.0-SNAPSHOT");
        return model;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
