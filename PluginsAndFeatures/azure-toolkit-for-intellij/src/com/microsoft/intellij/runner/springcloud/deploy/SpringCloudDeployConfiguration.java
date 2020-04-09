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

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.ProvisioningState;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.RuntimeVersion;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.ServiceResource;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.intellij.common.CommonConst;
import com.microsoft.intellij.runner.AzureRunConfigurationBase;
import com.microsoft.intellij.runner.functions.core.JsonUtils;
import com.microsoft.intellij.runner.springcloud.SpringCloudModel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SpringCloudDeployConfiguration extends AzureRunConfigurationBase<SpringCloudModel> {
    private static final String NEED_SPECIFY_PROJECT = "Please select a maven project";
    private static final String NEED_SPECIFY_SUBSCRIPTION = "Please select your subscription.";
    private static final String NEED_SPECIFY_CLUSTER = "Please select a target cluster.";
    private static final String NEED_SPECIFY_APP_NAME = "Please select a target app.";
    private static final String SERVICE_IS_NOT_READY = "Service is not ready for deploy, current status is ";
    private static final String TARGET_CLUSTER_DOES_NOT_EXISTS = "Target cluster does not exists.";
    private static final String TARGET_CLUSTER_IS_NOT_AVAILABLE = "Target cluster cannot be found in current subscription";

    private final Map<String, ServiceResource> serviceResourceMap = new HashMap<>();

    private final SpringCloudModel springCloudModel;

    public SpringCloudDeployConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        springCloudModel = new SpringCloudModel();
    }

    protected SpringCloudDeployConfiguration(@NotNull SpringCloudDeployConfiguration source) {
        super(source);
        this.springCloudModel = JsonUtils.deepCopyWithJson(source.getModel());
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
        // we need the jar built by <spring-boot-maven-plugin>, not the default output jar
        return null;
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
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new SpringCloudDeploymentState(getProject(), new SpringCloudDeployConfiguration(this));
    }

    public boolean isPublic() {
        return springCloudModel.isPublic();
    }

    public String getClusterId() {
        return springCloudModel.getClusterId();
    }

    public boolean isCreateNewApp() {
        return springCloudModel.isCreateNewApp();
    }

    public String getAppName() {
        return springCloudModel.getAppName();
    }

    public RuntimeVersion getRuntimeVersion() {
        final String runtimeString = springCloudModel.getRuntimeVersion();
        return StringUtils.isEmpty(runtimeString) ? null : RuntimeVersion.fromString(runtimeString);
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

    public String getJvmOptions() {
        return springCloudModel.getJvmOptions();
    }

    public String getProjectName() {
        return springCloudModel.getProjectName();
    }

    public boolean isEnablePersistentStorage() {
        return springCloudModel.isEnablePersistentStorage();
    }

    public Map<String, String> getEnvironment() {
        return springCloudModel.getEnvironment();
    }

    public void setPublic(boolean isPublic) {
        springCloudModel.setPublic(isPublic);
    }

    public void setSubscriptionId(String subscriptionId) {
        springCloudModel.setSubscriptionId(subscriptionId);
    }

    public void setClusterId(String clusterId) {
        springCloudModel.setClusterId(clusterId);
    }

    public void setAppName(String appName) {
        springCloudModel.setAppName(appName);
    }

    public void setCreateNewApp(boolean isNewApp) {
        springCloudModel.setCreateNewApp(isNewApp);
    }

    public void saveRuntimeVersion(RuntimeVersion runtimeVersion) {
        springCloudModel.setRuntimeVersion(Objects.toString(runtimeVersion, null));
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

    public void setJvmOptions(String jvmOptions) {
        springCloudModel.setJvmOptions(jvmOptions);
    }

    public void setEnablePersistentStorage(boolean enablePersistentStorage) {
        springCloudModel.setEnablePersistentStorage(enablePersistentStorage);
    }

    public void setEnvironment(Map<String, String> environment) {
        springCloudModel.setEnvironment(environment);
    }

    public void setProjectName(String moduleName) {
        springCloudModel.setProjectName(moduleName);
    }

    @Override
    public void validate() throws ConfigurationException {
        if (!AuthMethodManager.getInstance().isSignedIn()) {
            throw new ConfigurationException(CommonConst.NEED_SIGN_IN);
        }
        if (StringUtils.isEmpty(getProjectName())) {
            throw new ConfigurationException(NEED_SPECIFY_PROJECT);
        }
        if (StringUtils.isEmpty(getSubscriptionId())) {
            throw new ConfigurationException(NEED_SPECIFY_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(getClusterId())) {
            throw new ConfigurationException(NEED_SPECIFY_CLUSTER);
        }
        if (StringUtils.isEmpty(getAppName())) {
            throw new ConfigurationException(NEED_SPECIFY_APP_NAME);
        }
        final ServiceResource serviceResource = serviceResourceMap.computeIfAbsent(getClusterId(), id -> {
            try {
                return AzureSpringCloudMvpModel.getClusterById(getSubscriptionId(), getClusterId());
            } catch (IOException e) {
                return null;
            }
        });
        if (serviceResource == null) {
            throw new ConfigurationException(TARGET_CLUSTER_DOES_NOT_EXISTS);
        }
        // SDK will return null inner service object if cluster exists in other subscription
        if (serviceResource.inner() == null) {
            throw new ConfigurationException(TARGET_CLUSTER_IS_NOT_AVAILABLE);
        }
        final ProvisioningState provisioningState = serviceResource.properties().provisioningState();
        if (provisioningState != ProvisioningState.SUCCEEDED) {
            throw new ConfigurationException(SERVICE_IS_NOT_READY + provisioningState.toString());
        }
    }

}
