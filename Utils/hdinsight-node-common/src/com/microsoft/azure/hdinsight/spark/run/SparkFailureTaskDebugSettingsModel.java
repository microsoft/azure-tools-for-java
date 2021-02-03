/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
