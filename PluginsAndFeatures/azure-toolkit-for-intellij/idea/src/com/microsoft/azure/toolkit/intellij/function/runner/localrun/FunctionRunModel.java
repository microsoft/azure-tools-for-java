/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.localrun;

import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.function.runner.IntelliJFunctionContext;

public class FunctionRunModel extends IntelliJFunctionContext {

    private Artifact artifact;
    private String debugOptions;
    private String stagingFolder;
    private String funcPath;
    private String hostJsonPath;
    private String localSettingsJsonPath;

    public Artifact getArtifact() {
        return artifact;
    }

    public void setArtifact(Artifact artifact) {
        this.artifact = artifact;
    }

    public String getDebugOptions() {
        return debugOptions;
    }

    public void setDebugOptions(String debugOptions) {
        this.debugOptions = debugOptions;
    }

    public String getStagingFolder() {
        return stagingFolder;
    }

    public void setStagingFolder(String stagingFolder) {
        this.stagingFolder = stagingFolder;
    }

    public String getFuncPath() {
        return funcPath;
    }

    public void setFuncPath(String funcPath) {
        this.funcPath = funcPath;
    }

    public String getHostJsonPath() {
        return hostJsonPath;
    }

    public void setHostJsonPath(String hostJsonPath) {
        this.hostJsonPath = hostJsonPath;
    }

    public String getLocalSettingsJsonPath() {
        return localSettingsJsonPath;
    }

    public void setLocalSettingsJsonPath(String localSettingsJsonPath) {
        this.localSettingsJsonPath = localSettingsJsonPath;
    }

}
