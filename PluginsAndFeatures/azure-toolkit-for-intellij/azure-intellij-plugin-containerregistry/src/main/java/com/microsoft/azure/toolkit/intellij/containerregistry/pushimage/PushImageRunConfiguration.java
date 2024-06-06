/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage;

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
import com.microsoft.azure.toolkit.intellij.containerregistry.IDockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.model.container.pojo.PushImageRunModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration.validateDockerHostConfiguration;
import static com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration.validateDockerImageConfiguration;

public class PushImageRunConfiguration extends AzureRunConfigurationBase<PushImageRunModel> implements IDockerPushConfiguration {
    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    public static final String CONTAINER_REGISTRY_VALIDATION = "Please specify the container registry";

    private final PushImageRunModel dataModel;

    protected PushImageRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        dataModel = new PushImageRunModel();
    }

    @Override
    public PushImageRunModel getModel() {
        return dataModel;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new PushImageRunSettingsEditor(this.getProject(), this);
    }

    /**
     * Validate input value.
     */
    @Override
    public void validate() throws ConfigurationException {
        if (dataModel == null) {
            throw new ConfigurationException(MISSING_MODEL);
        }
        if (StringUtils.isEmpty(getContainerRegistryId())) {
            throw new ConfigurationException(CONTAINER_REGISTRY_VALIDATION);
        }
        validateDockerHostConfiguration(getDockerHostConfiguration());
        validateDockerImageConfiguration(getDockerImageConfiguration());
    }

    @Override
    public String getSubscriptionId() {
        return "";
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new PushImageRunState(getProject(), this);
    }

    @Override
    public String getTargetPath() {
        return dataModel.getTargetPath();
    }

    public void setTargetPath(String targetPath) {
        dataModel.setTargetPath(targetPath);
    }

    @Override
    public String getTargetName() {
        return dataModel.getTargetName();
    }

    public void setTargetName(String targetName) {
        dataModel.setTargetName(targetName);
    }

    public String getDockerFilePath() {
        return dataModel.getDockerFilePath();
    }

    public void setDockerFilePath(String dockerFilePath) {
        dataModel.setDockerFilePath(dockerFilePath);
    }

    public PrivateRegistryImageSetting getPrivateRegistryImageSetting() {
        return dataModel.getPrivateRegistryImageSetting();
    }

    public void setPrivateRegistryImageSetting(PrivateRegistryImageSetting privateRegistryImageSetting) {
        dataModel.setPrivateRegistryImageSetting(privateRegistryImageSetting);
    }

    public String getContainerRegistryId() {
        return this.dataModel.getContainerRegistryId();
    }

    @Override
    public String getFinalRepositoryName() {
        return getModel().getFinalRepositoryName();
    }

    @Override
    public String getFinalTagName() {
        return getModel().getFinalTagName();
    }

    public void setFinalRepositoryName(final String value) {
        getModel().setFinalRepositoryName(value);
    }

    public void setFinalTagName(final String value) {
        getModel().setFinalTagName(value);
    }

    public void setContainerRegistryId(String id) {
        this.dataModel.setContainerRegistryId(id);
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

    @javax.annotation.Nullable
    @Override
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

    @javax.annotation.Nullable
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
}
