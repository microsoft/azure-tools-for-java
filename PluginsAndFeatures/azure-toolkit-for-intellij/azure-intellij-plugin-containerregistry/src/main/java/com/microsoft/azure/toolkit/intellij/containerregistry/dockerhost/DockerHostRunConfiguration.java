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
import com.microsoft.azure.toolkit.intellij.containerregistry.IDockerConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunConfigurationBase;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DockerHostRunConfiguration extends AzureRunConfigurationBase<DockerHostRunSetting> implements IDockerConfiguration {

    private static final String MISSING_MODEL = "Configuration data model not initialized.";
    private static final String INVALID_DOCKER_HOST = "Please specify a valid docker host.";
    private static final String INVALID_DOCKER_FILE = "Please specify a valid docker file.";
    private static final String INVALID_CERT_PATH = "Please specify a valid certificate path.";
    private static final String MISSING_IMAGE_WITH_TAG = "Please specify Image and Tag.";
    private static final String CANNOT_END_WITH_COLON = "Image and tag name cannot end with ':'";
    public static final String REPO_LENGTH_INVALID = "The length of repository name must be at least one character "
            + "and less than 256 characters";
    public static final String CANNOT_END_WITH_SLASH = "The repository name should not end with '/'";
    public static final String REPO_COMPONENT_INVALID = "Invalid repository component: %s, should follow: %s";
    public static final String TAG_LENGTH_INVALID = "The length of tag name must be at least one character " +
            "and less than 128 characters";
    public static final String TAG_INVALID = "Invalid tag: %s, should follow: %s";
    public static final String DOMAIN_NAME_REGEX = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$";
    public static final String REPO_COMPONENTS_REGEX = "[a-z0-9]+(?:[._-][a-z0-9]+)*";
    public static final String TAG_REGEX = "^[\\w]+[\\w.-]*$";
    public static final int TAG_LENGTH = 128;
    public static final int REPO_LENGTH = 255;
    private final DockerHostRunSetting dataModel;

    protected DockerHostRunConfiguration(@Nonnull Project project, @Nonnull ConfigurationFactory factory, String name) {
        super(project, factory, name);
        dataModel = new DockerHostRunSetting();
    }

    @Override
    public DockerHostRunSetting getModel() {
        return dataModel;
    }

    @Nonnull
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
        validateDockerHostConfiguration(getDockerHostConfiguration());
        validateDockerImageConfiguration(getDockerImageConfiguration());
    }

    public static void validateDockerHostConfiguration(@Nullable DockerHost host) throws ConfigurationException {
        if (host == null) {
            throw new ConfigurationException(INVALID_DOCKER_HOST);
        }
        // docker host
        if (StringUtils.isEmpty(host.getDockerHost())) {
            throw new ConfigurationException(INVALID_DOCKER_HOST);
        }
        if (host.isTlsEnabled() && StringUtils.isEmpty(host.getDockerCertPath())) {
            throw new ConfigurationException(INVALID_CERT_PATH);
        }
    }

    public static void validateDockerImageConfiguration(@Nullable DockerImage image) throws ConfigurationException {
        if (image == null) {
            throw new ConfigurationException(INVALID_DOCKER_FILE);
        }
        if (image.isDraft() && (image.getDockerFile() == null || !Files.exists(Path.of(image.getDockerFile())))) {
            throw new ConfigurationException(INVALID_DOCKER_FILE);
        }
        final String imageTag = image.getImageName();
        if (StringUtils.isEmpty(imageTag)) {
            throw new ConfigurationException(MISSING_IMAGE_WITH_TAG);
        }
        // check repository first
        final String repositoryName = image.getRepositoryName();
        if (StringUtils.isBlank(repositoryName) || repositoryName.length() < 1 || repositoryName.length() > REPO_LENGTH) {
            throw new ConfigurationException(REPO_LENGTH_INVALID);
        }
        if (repositoryName.endsWith("/")) {
            throw new ConfigurationException(CANNOT_END_WITH_SLASH);
        }
        final String[] repoComponents = repositoryName.split("/");
        for (final String component : repoComponents) {
            if (!component.matches(REPO_COMPONENTS_REGEX)) {
                throw new ConfigurationException(String.format(REPO_COMPONENT_INVALID, component, REPO_COMPONENTS_REGEX));
            }
        }
        // check when contains tag
        final String tagName = image.getTagName();
        if (StringUtils.isBlank(tagName) || tagName.length() > TAG_LENGTH) {
            throw new ConfigurationException(TAG_LENGTH_INVALID);
        }
        if (!tagName.matches(TAG_REGEX)) {
            throw new ConfigurationException(String.format(TAG_INVALID, tagName, TAG_REGEX));
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment executionEnvironment) {
        return new DockerHostRunState(getProject(), dataModel);
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

    @Nullable
    public DockerImage getDockerImageConfiguration() {
        if (StringUtils.isAllBlank(getImageName(), getDockerFilePath())) {
            return null;
        }
        final DockerImage image = new DockerImage();
        image.setDraft(false);
        image.setRepositoryName(getImageName());
        image.setTagName(getTagName());
        image.setDockerFile(getDockerFilePath());
        image.setDraft(StringUtils.isNoneBlank(this.getDockerFilePath()));
        return image;
    }

    public DockerHost getDockerHostConfiguration() {
        return StringUtils.isNoneBlank(getDockerHost()) ? new DockerHost(getDockerHost(), getDockerCertPath()) : null;
    }

    public void setDockerImage(@Nullable DockerImage image) {
        this.setImageName(Optional.ofNullable(image).map(DockerImage::getRepositoryName).orElse(null));
        this.setTagName(Optional.ofNullable(image).map(DockerImage::getTagName).orElse(null));
        this.setDockerFilePath(Optional.ofNullable(image).map(DockerImage::getDockerFile).orElse(null));
    }

    public void setHost(@Nullable DockerHost host) {
        this.setDockerHost(Optional.ofNullable(host).map(DockerHost::getDockerHost).orElse(null));
        this.setDockerCertPath(Optional.ofNullable(host).map(DockerHost::getDockerCertPath).orElse(null));
        this.setTlsEnabled(Optional.ofNullable(host).map(DockerHost::isTlsEnabled).orElse(false));
    }

    public Integer getPort() {
        return getModel().getPort();
    }

    public void setPort(@Nonnull final Integer number) {
        getModel().setPort(number);
    }
}
