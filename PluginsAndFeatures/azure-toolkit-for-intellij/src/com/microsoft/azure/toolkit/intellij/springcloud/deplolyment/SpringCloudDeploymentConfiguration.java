/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2020_07_01.ProvisioningState;
import com.microsoft.azure.management.appplatform.v2020_07_01.ServiceResource;
import com.microsoft.azure.toolkit.intellij.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingsEditor;
import com.microsoft.azure.toolkit.intellij.springcloud.SpringCloudModel;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SpringCloudDeploymentConfiguration extends AzureRunConfigurationBase<SpringCloudModel> {
    private static final String NEED_SPECIFY_ARTIFACT = "Please select an artifact";
    private static final String NEED_SPECIFY_SUBSCRIPTION = "Please select your subscription.";
    private static final String NEED_SPECIFY_CLUSTER = "Please select a target cluster.";
    private static final String NEED_SPECIFY_APP_NAME = "Please select a target app.";
    private static final String SERVICE_IS_NOT_READY = "Service is not ready for deploy, current status is ";
    private static final String TARGET_CLUSTER_DOES_NOT_EXISTS = "Target cluster does not exists.";
    private static final String TARGET_CLUSTER_IS_NOT_AVAILABLE = "Target cluster cannot be found in current subscription";

    private static LoadingCache<String, ServiceResource> clusterStatusCache = CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, ServiceResource>() {
            public ServiceResource load(String clusterId) {
                final String subscriptionId = AzureMvpModel.getSegment(clusterId, "subscriptions");
                return AzureSpringCloudMvpModel.getClusterById(subscriptionId, clusterId);
            }
        });

    private final SpringCloudModel model;

    public SpringCloudDeploymentConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        model = new SpringCloudModel();
    }

    protected SpringCloudDeploymentConfiguration(@NotNull SpringCloudDeploymentConfiguration source) {
        super(source);
        this.model = JsonUtils.deepCopyWithJson(source.getModel());
    }

    public String getArtifactIdentifier() {
        return model.getArtifactIdentifier();
    }

    @Override
    public SpringCloudModel getModel() {
        return this.model;
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
        return model.getSubscriptionId();
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new Editor(this, getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new SpringCloudDeploymentConfigurationState(getProject(), new SpringCloudDeploymentConfiguration(this));
    }

    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        if (StringUtils.isEmpty(getArtifactIdentifier())) {
            throw new ConfigurationException(NEED_SPECIFY_ARTIFACT);
        }
        if (StringUtils.isEmpty(getSubscriptionId())) {
            throw new ConfigurationException(NEED_SPECIFY_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(this.model.getClusterId())) {
            throw new ConfigurationException(NEED_SPECIFY_CLUSTER);
        }
        if (StringUtils.isEmpty(this.model.getAppName())) {
            throw new ConfigurationException(NEED_SPECIFY_APP_NAME);
        }
        final ServiceResource serviceResource;
        try {
            serviceResource = clusterStatusCache.get(this.model.getClusterId());
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
        } catch (ExecutionException e) {
            // swallow validation exceptions
        }
    }

    public SpringCloudDeploymentConfig getDeployment() {
        return SpringCloudDeploymentConfig.builder()
            .cpu(this.model.getCpu())
            .memoryInGB(this.model.getMemoryInGB())
            .instanceCount(this.model.getInstanceCount())
            .deploymentName(this.model.getDeploymentName())
            .jvmOptions(this.model.getJvmOptions())
            .runtimeVersion(this.model.getRuntimeVersion())
            .enablePersistentStorage(this.model.isEnablePersistentStorage())
            .environment(this.model.getEnvironment())
            .build();
    }

    public static class Factory extends ConfigurationFactory {
        public static final String FACTORY_NAME = "Deploy Spring Cloud Services";
        public Factory(@NotNull ConfigurationType type) {
            super(type);
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new SpringCloudDeploymentConfiguration(project, this, project.getName());
        }

        @Override
        public RunConfiguration createConfiguration(String name, RunConfiguration template) {
            return new SpringCloudDeploymentConfiguration(template.getProject(), this, name);
        }

        @Override
        public String getName() {
            return FACTORY_NAME;
        }
    }

    static class Editor extends AzureSettingsEditor<SpringCloudDeploymentConfiguration> {
        private final AzureSettingPanel<SpringCloudDeploymentConfiguration> mainPanel;

        Editor(SpringCloudDeploymentConfiguration configuration, Project project) {
            super(project);
            this.mainPanel = new SpringCloudDeploymentConfigurationPanel(project, configuration);
        }

        protected void disposeEditor() {
            this.mainPanel.disposeEditor();
        }

        @Override
        @NotNull
        protected AzureSettingPanel<SpringCloudDeploymentConfiguration> getPanel() {
            return this.mainPanel;
        }

        @Override
        protected void resetEditorFrom(@NotNull SpringCloudDeploymentConfiguration conf) {
            this.getPanel().reset(conf);
        }
    }
}
