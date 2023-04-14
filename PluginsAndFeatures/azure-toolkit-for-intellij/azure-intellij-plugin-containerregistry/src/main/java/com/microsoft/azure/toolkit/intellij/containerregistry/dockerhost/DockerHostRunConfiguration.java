/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost;

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
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.IDockerConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

public class DockerHostRunConfiguration extends AzureRunConfigurationBase<DockerHostRunSetting> implements IDockerConfiguration {
    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String INVALID_WAR_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String INVALID_DOCKER_HOST = "Please specify a valid docker host.";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    private static final String INVALID_CERT_PATH = "Please specify a valid certificate path.";
    private static final String MISSING_IMAGE_NAME = "Please specify a valid image name.";
    private final DockerHostRunSetting dataModel;

    protected DockerHostRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        dataModel = new DockerHostRunSetting();
    }

    @Override
    public DockerHostRunSetting getModel() {
        return dataModel;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DockerHostRunSettingsEditor(this.getProject(), this);
    }

    /**
     * Validate input value.
     */
    @Override
    public void validate() throws ConfigurationException {
        // TODO: add more
        if (dataModel == null) {
            throw new ConfigurationException(MISSING_MODEL);
        }
        // docker host
        if (StringUtils.isEmpty(dataModel.getDockerHost())) {
            throw new ConfigurationException(INVALID_DOCKER_HOST);
        }
        if (dataModel.isTlsEnabled() && StringUtils.isEmpty(dataModel.getDockerCertPath())) {
            throw new ConfigurationException(INVALID_CERT_PATH);
        }
        if (StringUtils.isEmpty(dataModel.getImageName())) {
            throw new ConfigurationException(MISSING_IMAGE_NAME);
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) {
        return new DockerHostRunState(getProject(), dataModel);
    }

    @javax.annotation.Nullable
    @Override
    public DockerImage getDockerImageConfiguration() {
        final DockerImage image = new DockerImage();
        image.setRepositoryName(this.getImageName());
        image.setTagName(this.getTagName());
        image.setDockerFile(Optional.ofNullable(this.getDockerFilePath()).map(File::new).orElse(null));
        image.setDraft(StringUtils.isNoneBlank(this.getDockerFilePath()));
        return image;
    }

    @javax.annotation.Nullable
    @Override
    public DockerHost getDockerHostConfiguration() {
        return new DockerHost(dataModel.getDockerHost(), dataModel.getDockerCertPath());
    }

    public String getDockerHost() {
        return dataModel.getDockerHost();
    }

    public void setDockerHost(String dockerHost) {
        dataModel.setDockerHost(dockerHost);
    }

    public String getDockerCertPath() {
        return dataModel.getDockerCertPath();
    }

    public void setDockerCertPath(String dockerCertPath) {
        dataModel.setDockerCertPath(dockerCertPath);
    }

    public String getDockerFilePath() {
        return dataModel.getDockerFilePath();
    }

    public void setDockerFilePath(String dockerFilePath) {
        dataModel.setDockerFilePath(dockerFilePath);
    }

    public boolean isTlsEnabled() {
        return dataModel.isTlsEnabled();
    }

    public void setTlsEnabled(boolean tlsEnabled) {
        dataModel.setTlsEnabled(tlsEnabled);
    }

    public String getImageName() {
        return dataModel.getImageName();
    }

    public void setImageName(String imageName) {
        dataModel.setImageName(imageName);
    }

    public String getTagName() {
        return dataModel.getTagName();
    }

    public void setTagName(String tagName) {
        dataModel.setTagName(tagName);
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

    @Override
    public String getSubscriptionId() {
        return "";
    }

    public void setDockerImage(@Nullable DockerImage image) {
        this.setImageName(Optional.ofNullable(image).map(DockerImage::getRepositoryName).orElse(null));
        this.setTagName(Optional.ofNullable(image).map(DockerImage::getTagName).orElse(null));
        this.setDockerFilePath(Optional.ofNullable(image).map(DockerImage::getDockerFile).map(File::getAbsolutePath).orElse(null));
    }

    public void setHost(@Nullable DockerHost host) {
        this.setDockerHost(Optional.ofNullable(host).map(DockerHost::getDockerHost).orElse(null));
        this.setDockerCertPath(Optional.ofNullable(host).map(DockerHost::getDockerCertPath).orElse(null));
        this.setTlsEnabled(Optional.ofNullable(host).map(DockerHost::isTlsEnabled).orElse(false));
    }
}
