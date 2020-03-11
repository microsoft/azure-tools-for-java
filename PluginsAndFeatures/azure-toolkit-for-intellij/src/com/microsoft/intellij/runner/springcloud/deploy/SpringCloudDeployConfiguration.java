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

package com.microsoft.intellij.runner.springcloud.deploy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.RuntimeVersion;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.springcloud.SpringCloudModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class SpringCloudDeployConfiguration extends AzureRunConfigurationBase<SpringCloudModel> {
    private final SpringCloudModel springCloudModel;

    public SpringCloudDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        springCloudModel = new SpringCloudModel();
    }

    @Override
    public SpringCloudModel getModel() {
        return this.springCloudModel;
    }

    @Override
    public String getTargetName() {
        return null;
    }

    @Override
    public String getTargetPath() {
        return springCloudModel.getArtifactPath();
    }

    @Override
    public String getSubscriptionId() {
        return springCloudModel.getSubscriptionId();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SpringCloudDeploymentSettingEditor(getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new SpringCloudDeploymentState(getProject(), this);
    }

    public String getArtifactPath() {
        return springCloudModel.getArtifactPath();
    }

    public boolean isPublic() {
        return springCloudModel.isPublic();
    }

    public String getResourceGroup() {
        return springCloudModel.getResourceGroup();
    }

    public String getClusterName() {
        return springCloudModel.getClusterName();
    }

    public String getAppName() {
        return springCloudModel.getAppName();
    }

    public RuntimeVersion getRuntimeVersion() {
        return springCloudModel.getRuntimeVersion();
    }

    public Integer getCpu() {
        return springCloudModel.getCpu();
    }

    public Integer getMemoryInGB() {
        return springCloudModel.getMemoryInGB();
    }

    public Integer getInstanceCount() {
        return springCloudModel.getInstanceCount();
    }

    public String getDeploymentName() {
        return springCloudModel.getDeploymentName();
    }

    public String getJvmOptions() {
        return springCloudModel.getJvmOptions();
    }

    public String getModuleName() {
        return springCloudModel.getModuleName();
    }

    public boolean isEnablePersistentStorage() {
        return springCloudModel.isEnablePersistentStorage();
    }

    public Map<String, String> getEnvironment() {
        return springCloudModel.getEnvironment();
    }

    public void setArtifactPath(String artifactPath) {
        springCloudModel.setArtifactPath(artifactPath);
    }

    public void setPublic(boolean isPublic) {
        springCloudModel.setPublic(isPublic);
    }

    public void setSubscriptionId(String subscriptionId) {
        springCloudModel.setSubscriptionId(subscriptionId);
    }

    public void setResourceGroup(String resourceGroup) {
        springCloudModel.setResourceGroup(resourceGroup);
    }

    public void setClusterName(String clusterName) {
        springCloudModel.setClusterName(clusterName);
    }

    public void setAppName(String appName) {
        springCloudModel.setAppName(appName);
    }

    public void setRuntimeVersion(RuntimeVersion runtimeVersion) {
        springCloudModel.setRuntimeVersion(runtimeVersion);
    }

    public void setCpu(Integer cpu) {
        springCloudModel.setCpu(cpu);
    }

    public void setMemoryInGB(Integer memoryInGB) {
        springCloudModel.setMemoryInGB(memoryInGB);
    }

    public void setInstanceCount(Integer instanceCount) {
        springCloudModel.setInstanceCount(instanceCount);
    }

    public void setDeploymentName(String deploymentName) {
        springCloudModel.setDeploymentName(deploymentName);
    }

    public void setJvmOptions(String jvmOptions) {
        springCloudModel.setJvmOptions(jvmOptions);
    }

    public void setEnablePersistentStorage(boolean enablePersistentStorage) {
        springCloudModel.setEnablePersistentStorage(enablePersistentStorage);
    }

    public void setEnvironment(Map<String, String> environment) {
        springCloudModel.setEnvironment(environment);
    }

    public void setModuleName(String moduleName) {
        springCloudModel.setModuleName(moduleName);
    }

    @Override
    public void validate() throws ConfigurationException {
    }
}
