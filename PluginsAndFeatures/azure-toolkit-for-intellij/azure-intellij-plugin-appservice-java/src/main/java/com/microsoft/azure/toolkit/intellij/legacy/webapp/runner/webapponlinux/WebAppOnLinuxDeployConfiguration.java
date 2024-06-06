/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.IDockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration.validateDockerHostConfiguration;
import static com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration.validateDockerImageConfiguration;
import static com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.PushImageRunConfiguration.CONTAINER_REGISTRY_VALIDATION;

public class WebAppOnLinuxDeployConfiguration extends AzureRunConfigurationBase<IntelliJWebAppOnLinuxDeployModel> implements IDockerPushConfiguration {

    private static final String MISSING_SERVER_URL = "Please specify a valid Server URL.";
    private static final String MISSING_USERNAME = "Please specify Username.";
    private static final String MISSING_PASSWORD = "Please specify Password.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String MISSING_WEB_APP = "Please specify Web App for Containers.";
    private static final String MISSING_SUBSCRIPTION = "Please specify Subscription.";
    private static final String MISSING_RESOURCE_GROUP = "Please specify Resource Group.";
    private static final String MISSING_APP_SERVICE_PLAN = "Please specify App Service Plan.";

    private final IntelliJWebAppOnLinuxDeployModel deployModel;

    protected WebAppOnLinuxDeployConfiguration(@Nonnull Project project, @Nonnull ConfigurationFactory factory, String
            name) {
        super(project, factory, name);
        deployModel = new IntelliJWebAppOnLinuxDeployModel();
    }

    @Override
    public IntelliJWebAppOnLinuxDeployModel getModel() {
        return this.deployModel;
    }

    @Override
    public String getTargetName() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getTargetPath() {
        return StringUtils.EMPTY;
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WebAppOnLinuxDeploySettingsEditor(this.getProject(), this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment executionEnvironment) {
        return new WebAppOnLinuxDeployState(getProject(), this);
    }

    /**
     * Configuration value Validation.
     */
    @Override
    public void validate() throws ConfigurationException {
        checkAzurePreconditions();
        validateDockerHostConfiguration(getDockerHostConfiguration());
        validateDockerImageConfiguration(getDockerImageConfiguration());
        // registry
        if (StringUtils.isEmpty(getContainerRegistryId())) {
            throw new ConfigurationException(CONTAINER_REGISTRY_VALIDATION);
        }
        // web app
        final AppServiceConfig config = deployModel.getConfig();
        if (StringUtils.isEmpty(config.getAppName())) {
            throw new ConfigurationException(MISSING_WEB_APP);
        }
        if (StringUtils.isEmpty(config.getSubscriptionId())) {
            throw new ConfigurationException(MISSING_SUBSCRIPTION);
        }
        if (StringUtils.isEmpty(config.resourceGroup())) {
            throw new ConfigurationException(MISSING_RESOURCE_GROUP);
        }
        if (StringUtils.isEmpty(config.servicePlanName())) {
            throw new ConfigurationException(MISSING_APP_SERVICE_PLAN);
        }

    }

    @Override
    public String getSubscriptionId() {
        return deployModel.getSubscriptionId();
    }

    public void setDockerImage(@Nullable DockerImage image) {
        final DockerHostRunSetting dockerHostRunSetting = Optional.ofNullable(getDockerHostRunSetting()).orElseGet(DockerHostRunSetting::new);
        dockerHostRunSetting.setImageName(Optional.ofNullable(image).map(DockerImage::getRepositoryName).orElse(null));
        dockerHostRunSetting.setTagName(Optional.ofNullable(image).map(DockerImage::getTagName).orElse(null));
        dockerHostRunSetting.setDockerFilePath(Optional.ofNullable(image).map(DockerImage::getDockerFile).orElse(null));
        this.getModel().setDockerHostRunSetting(dockerHostRunSetting);
    }

    public void setHost(@Nullable DockerHost host) {
        final DockerHostRunSetting dockerHostRunSetting = Optional.ofNullable(getDockerHostRunSetting()).orElseGet(DockerHostRunSetting::new);
        dockerHostRunSetting.setDockerHost(Optional.ofNullable(host).map(DockerHost::getDockerHost).orElse(null));
        dockerHostRunSetting.setDockerCertPath(Optional.ofNullable(host).map(DockerHost::getDockerCertPath).orElse(null));
        dockerHostRunSetting.setTlsEnabled(Optional.ofNullable(host).map(DockerHost::isTlsEnabled).orElse(false));
        this.getModel().setDockerHostRunSetting(dockerHostRunSetting);
    }

    public DockerImage getDockerImageConfiguration() {
        final DockerImage image = new DockerImage();
        final DockerHostRunSetting dockerHostRunSetting = getDockerHostRunSetting();
        if (dockerHostRunSetting == null || StringUtils.isAllBlank(dockerHostRunSetting.getImageName(), dockerHostRunSetting.getDockerFilePath())) {
            return null;
        }
        image.setRepositoryName(dockerHostRunSetting.getImageName());
        image.setTagName(dockerHostRunSetting.getTagName());
        image.setDockerFile(Optional.ofNullable(dockerHostRunSetting.getDockerFilePath()).orElse(null));
        image.setDraft(StringUtils.isNoneBlank(dockerHostRunSetting.getDockerFilePath()));
        return image;
    }

    @Nullable
    @Override
    public DockerHost getDockerHostConfiguration() {
        final DockerHostRunSetting dockerHostRunSetting = getDockerHostRunSetting();
        if (dockerHostRunSetting == null || StringUtils.isEmpty(dockerHostRunSetting.getDockerHost())) {
            return null;
        }
        return new DockerHost(dockerHostRunSetting.getDockerHost(), dockerHostRunSetting.getDockerCertPath());
    }

    @Nullable
    public DockerHostRunSetting getDockerHostRunSetting() {
        return getModel().getDockerHostRunSetting();
    }

    public String getContainerRegistryId() {
        return getModel().getContainerRegistryId();
    }

    @Override
    public String getFinalRepositoryName() {
        return getModel().getFinalRepositoryName();
    }

    @Override
    public String getFinalTagName() {
        return getModel().getFinalTagName();
    }

    public Integer getPort() {
        return getModel().getPort();
    }

    public void setPort(final Integer port) {
        getModel().setPort(port);
    }

    public AppServiceConfig getAppServiceConfig() {
        return getModel().getConfig();
    }

    public void setWebAppConfig(@Nonnull final AppServiceConfig webAppConfig) {
        getModel().setConfig(webAppConfig);
    }

    public DockerPushConfiguration getDockerPushConfiguration() {
        final DockerPushConfiguration dockerPushConfiguration = new DockerPushConfiguration();
        dockerPushConfiguration.setContainerRegistryId(this.getContainerRegistryId());
        dockerPushConfiguration.setDockerHost(this.getDockerHostConfiguration());
        dockerPushConfiguration.setDockerImage(this.getDockerImageConfiguration());
        return dockerPushConfiguration;
    }

    public void setDockerPushConfiguration(@Nonnull final DockerPushConfiguration configuration) {
        this.setHost(configuration.getDockerHost());
        this.setDockerImage(configuration.getDockerImage());
        this.setFinalRepositoryName(configuration.getFinalRepositoryName());
        this.setFinalTagName(configuration.getFinalTagName());
        this.getModel().setContainerRegistryId(configuration.getContainerRegistryId());
    }

    public void setFinalRepositoryName(final String value) {
        getModel().setFinalRepositoryName(value);
    }

    public void setFinalTagName(final String value) {
        getModel().setFinalTagName(value);
    }
}
