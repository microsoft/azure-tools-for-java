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

package com.microsoft.intellij.runner.springcloud;

import java.util.HashMap;
import java.util.Map;

public class SpringCloudModel {
    private boolean isCreateNewApp;
    private String artifactIdentifier;
    // app
    private boolean isPublic;
    private String subscriptionId;
    private String clusterId;
    private String appName;
    private String runtimeVersion;
    // deployment
    private Integer cpu;
    private Integer memoryInGB;
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private boolean enablePersistentStorage;
    private Map<String, String> environment;

    public boolean isCreateNewApp() {
        return isCreateNewApp;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getAppName() {
        return appName;
    }

    public String getRuntimeVersion() {
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

    public String getJvmOptions() {
        return jvmOptions;
    }

    public String getClusterId() {
        return clusterId;
    }

    public boolean isEnablePersistentStorage() {
        return enablePersistentStorage;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setRuntimeVersion(String runtimeVersion) {
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

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public void setEnablePersistentStorage(boolean enablePersistentStorage) {
        this.enablePersistentStorage = enablePersistentStorage;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public void setCreateNewApp(boolean createNewApp) {
        isCreateNewApp = createNewApp;
    }

    public void setClusterId(final String clusterId) {
        this.clusterId = clusterId;
    }

    public String getArtifactIdentifier() {
        return artifactIdentifier;
    }

    public void setArtifactIdentifier(final String artifactIdentifier) {
        this.artifactIdentifier = artifactIdentifier;
    }

    public Map<String, String> getTelemetryProperties() {
        HashMap result = new HashMap();
        try {
            result.put("runtime", this.getRuntimeVersion());
            result.put("subscriptionId", this.getSubscriptionId());
            result.put("isCreateNew", String.valueOf(this.isCreateNewApp()));
        } catch (Exception e) {
            // swallow exception as telemetry should not break users operation
        }
        return result;
    }
}
