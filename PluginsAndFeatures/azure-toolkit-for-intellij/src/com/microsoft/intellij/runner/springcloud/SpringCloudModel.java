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
 *
 */

package com.microsoft.intellij.runner.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.RuntimeVersion;

import java.util.Map;

public class SpringCloudModel {

    private String moduleName;
    private String artifactPath;
    // app
    private boolean isPublic;
    private String subscriptionId;
    private String resourceGroup;
    private String clusterName;
    private String appName;
    private RuntimeVersion runtimeVersion;
    // deployment
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private boolean enablePersistentStorage;
    private Map<String, String> environment;

    public String getArtifactPath() {
        return artifactPath;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public RuntimeVersion getRuntimeVersion() {
        return runtimeVersion;
    }

    public Integer getCpu() {
        return cpu;
    }

    public Integer getMemoryInGB() {
        return memoryInGB;
    }

    public Integer getInstanceCount() {
        return instanceCount;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public String getModuleName() {
        return moduleName;
    }

    public boolean isEnablePersistentStorage() {
        return enablePersistentStorage;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setArtifactPath(String artifactPath) {
        this.artifactPath = artifactPath;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setRuntimeVersion(RuntimeVersion runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public void setMemoryInGB(Integer memoryInGB) {
        this.memoryInGB = memoryInGB;
    }

    public void setInstanceCount(Integer instanceCount) {
        this.instanceCount = instanceCount;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public void setEnablePersistentStorage(boolean enablePersistentStorage) {
        this.enablePersistentStorage = enablePersistentStorage;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }
}
