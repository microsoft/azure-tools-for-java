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

package com.microsoft.intellij.helpers.springcloud;

import java.util.List;
import java.util.Map;

public class SpringAppViewModel {
    private String subscriptionName;
    private String resourceGroup;
    private String region;
    private String clusterName;
    private String appName;
    private String runtimeVersion;

    private Integer cpu;
    private Integer memoryInGB;
    private String jvmOptions;

    private List<SpringAppInstanceViewModel> instance;

    private String javaVersion;
    private boolean enablePersistentStorage;
    private boolean enablePublicUrl;
    private Map<String, String> environment;

    // Read only
    private String publicUrl;
    private String testUrl;

    private String status;

    private Integer upInstanceCount;
    private Integer downInstanceCount;

    private Integer totalStorageInGB;
    private Integer usedStorageInGB;
    private String persistentMountPath;

    private boolean canStart;
    private boolean canStop;
    private boolean canReStart;

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getRuntimeVersion() {
        return runtimeVersion;
    }

    public void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Integer getMemoryInGB() {
        return memoryInGB;
    }

    public void setMemoryInGB(Integer memoryInGB) {
        this.memoryInGB = memoryInGB;
    }

    public String getJvmOptions() {
        return jvmOptions;
    }

    public void setJvmOptions(String jvmOptions) {
        this.jvmOptions = jvmOptions;
    }

    public boolean isEnablePersistentStorage() {
        return enablePersistentStorage;
    }

    public void setEnablePersistentStorage(boolean enablePersistentStorage) {
        this.enablePersistentStorage = enablePersistentStorage;
    }

    public boolean isEnablePublicUrl() {
        return enablePublicUrl;
    }

    public void setEnablePublicUrl(boolean enablePublicUrl) {
        this.enablePublicUrl = enablePublicUrl;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getTestUrl() {
        return testUrl;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUpInstanceCount() {
        return upInstanceCount;
    }

    public void setUpInstanceCount(Integer upInstanceCount) {
        this.upInstanceCount = upInstanceCount;
    }

    public Integer getDownInstanceCount() {
        return downInstanceCount;
    }

    public void setDownInstanceCount(Integer downInstanceCount) {
        this.downInstanceCount = downInstanceCount;
    }

    public Integer getTotalStorageInGB() {
        return totalStorageInGB;
    }

    public void setTotalStorageInGB(Integer totalStorageInGB) {
        this.totalStorageInGB = totalStorageInGB;
    }

    public Integer getUsedStorageInGB() {
        return usedStorageInGB;
    }

    public void setUsedStorageInGB(Integer usedStorageInGB) {
        this.usedStorageInGB = usedStorageInGB;
    }

    public boolean isCanStart() {
        return canStart;
    }

    public void setCanStart(boolean canStart) {
        this.canStart = canStart;
    }

    public boolean isCanStop() {
        return canStop;
    }

    public void setCanStop(boolean canStop) {
        this.canStop = canStop;
    }

    public boolean isCanReStart() {
        return canReStart;
    }

    public void setCanReStart(boolean canReStart) {
        this.canReStart = canReStart;
    }

    public String getPersistentMountPath() {
        return persistentMountPath;
    }

    public void setPersistentMountPath(String persistentMountPath) {
        this.persistentMountPath = persistentMountPath;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public List<SpringAppInstanceViewModel> getInstance() {
        return instance;
    }

    public void setInstance(List<SpringAppInstanceViewModel> instance) {
        this.instance = instance;
    }
}
