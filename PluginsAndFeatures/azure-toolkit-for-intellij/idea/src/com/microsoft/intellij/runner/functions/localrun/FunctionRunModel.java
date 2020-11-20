/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.functions.localrun;

import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.intellij.runner.functions.IntelliJFunctionContext;

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
