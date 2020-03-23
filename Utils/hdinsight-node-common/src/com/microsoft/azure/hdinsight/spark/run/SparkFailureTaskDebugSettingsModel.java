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

package com.microsoft.azure.hdinsight.spark.run;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.HashMap;
import java.util.Map;

public class SparkFailureTaskDebugSettingsModel implements Cloneable {
    @Nullable
    private String failureContextPath;

    private boolean isPassParentEnvs = true;

    @Nullable
    private String programParameters;

    @NotNull
    private Map<String, String> envs = new HashMap<>();

    @Nullable
    private String vmParameters;

    @Nullable
    private String log4jProperties;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }

    // Getters / Setters

    @Nullable
    public String getFailureContextPath() {
        return failureContextPath;
    }

    public void setFailureContextPath(@Nullable String failureContextPath) {
        this.failureContextPath = failureContextPath;
    }

    public boolean isPassParentEnvs() {
        return isPassParentEnvs;
    }

    public void setPassParentEnvs(boolean passParentEnvs) {
        isPassParentEnvs = passParentEnvs;
    }

    @Nullable
    public String getProgramParameters() {
        return programParameters;
    }

    public void setProgramParameters(@Nullable String programParameters) {
        this.programParameters = programParameters;
    }

    @NotNull
    public Map<String, String> getEnvs() {
        return envs;
    }

    public void setEnvs(@NotNull Map<String, String> envs) {
        this.envs = envs;
    }

    @Nullable
    public String getVmParameters() {
        return vmParameters;
    }

    public void setVmParameters(@Nullable String vmParameters) {
        this.vmParameters = vmParameters;
    }

    @Nullable
    public String getLog4jProperties() {
        return log4jProperties;
    }

    public void setLog4jProperties(@Nullable String log4jProperties) {
        this.log4jProperties = log4jProperties;
    }
}
