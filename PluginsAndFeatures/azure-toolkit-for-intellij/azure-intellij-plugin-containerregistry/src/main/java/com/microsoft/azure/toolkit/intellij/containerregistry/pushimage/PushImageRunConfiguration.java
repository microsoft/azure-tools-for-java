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
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.IDockerConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import com.microsoft.azuretools.core.mvp.model.container.pojo.PushImageRunModel;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class PushImageRunConfiguration extends AzureRunConfigurationBase<PushImageRunModel> implements IDockerConfiguration {
    // TODO: move to util
    private static final String MISSING_ARTIFACT = "A web archive (.war) artifact has not been configured.";
    private static final String MISSING_SERVER_URL = "Please specify a valid Server URL.";
    private static final String MISSING_USERNAME = "Please specify Username.";
    private static final String MISSING_PASSWORD = "Please specify Password.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    private static final String INVALID_IMAGE_WITH_TAG = "Image and Tag name is invalid";
    private static final String INVALID_ARTIFACT_FILE = "The artifact name %s is invalid. "
            + "An artifact name may contain only the ASCII letters 'a' through 'z' (case-insensitive), "
            + "and the digits '0' through '9', '.', '-' and '_'.";
    private static final String CANNOT_END_WITH_COLON = "Image and tag name cannot end with ':'";
    private static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    private static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    private static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    private static final String TAG_LENGTH_INVALID = "The length of tag name must be no more than 128 characters";
    private static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    private static final String ARTIFACT_NAME_REGEX = "^[.A-Za-z0-9_-]+\\.(war|jar)$";
    private static final String DOMAIN_NAME_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    private static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    private static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    private static final int TAG_LENGTH = 128;
    private static final int REPO_LENGTH = 255;

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

    public void setDockerImage(@Nullable DockerImage image) {
        final DockerHostRunSetting dockerHostRunSetting = Optional.ofNullable(getDockerHostRunSetting()).orElseGet(DockerHostRunSetting::new);
        dockerHostRunSetting.setImageName(Optional.ofNullable(image).map(DockerImage::getRepositoryName).orElse(null));
        dockerHostRunSetting.setTagName(Optional.ofNullable(image).map(DockerImage::getTagName).orElse(null));
        dockerHostRunSetting.setDockerFilePath(Optional.ofNullable(image).map(DockerImage::getDockerFile).map(File::getAbsolutePath).orElse(null));
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
        image.setDockerFile(Optional.ofNullable(dockerHostRunSetting.getDockerFilePath()).map(File::new).orElse(null));
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

    @javax.annotation.Nullable
    @Override
    public String getRegistryUrl() {
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(getContainerRegistryId());
        return Optional.ofNullable(registry).map(ContainerRegistry::getLoginServerUrl).orElse(null);
    }

    @Nullable
    public DockerHostRunSetting getDockerHostRunSetting() {
        return getModel().getDockerHostRunSetting();
    }
}
