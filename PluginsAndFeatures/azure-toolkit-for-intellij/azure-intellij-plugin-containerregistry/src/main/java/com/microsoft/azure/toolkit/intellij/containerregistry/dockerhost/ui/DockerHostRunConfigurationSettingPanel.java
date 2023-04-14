/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.container.model.DockerConfiguration;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.File;
import java.util.Optional;

public class DockerHostRunConfigurationSettingPanel extends AzureSettingPanel<DockerHostRunConfiguration> {
    private JPanel pnlRoot;
    private DockerImageConfigurationPanel pnlConfiguration;
    private final Project project;
    private final DockerHostRunConfiguration runConfiguration;

    public DockerHostRunConfigurationSettingPanel(@Nonnull Project project, DockerHostRunConfiguration runConfiguration) {
        super(project);
        this.project = project;
        this.runConfiguration = runConfiguration;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        final AzureFormInput.AzureValueChangeListener<DockerImage> runnable = image -> AzureTaskManager.getInstance().runLater(() ->
                DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(DataManager.getInstance().getDataContext(getMainPanel()), this.runConfiguration, image), AzureTask.Modality.ANY);
        this.pnlConfiguration.addImageListener(runnable);
    }

    @Override
    public @Nonnull String getPanelName() {
        return "Docker Run";
    }

    @Override
    public void disposeEditor() {

    }

    @Override
    protected void resetFromConfig(@Nonnull DockerHostRunConfiguration data) {
        final DockerPushConfiguration dockerConfiguration = new DockerPushConfiguration();
        if (StringUtils.isNoneBlank(data.getDockerHost())) {
            dockerConfiguration.setDockerHost(new DockerHost(data.getDockerHost(), data.getDockerCertPath()));
        }
        if (StringUtils.isNotEmpty(data.getImageName()) || StringUtils.isNotEmpty(data.getDockerFilePath())) {
            final DockerImage image = new DockerImage();
            image.setRepositoryName(data.getImageName());
            image.setTagName(data.getTagName());
            image.setDockerFile(Optional.ofNullable(data.getDockerFilePath()).map(File::new).orElse(null));
            dockerConfiguration.setDockerImage(image);
        }
        pnlConfiguration.setValue(dockerConfiguration);
    }

    @Override
    protected void apply(@Nonnull DockerHostRunConfiguration configuration) {
        final DockerConfiguration dockerConfiguration = pnlConfiguration.getValue();
        configuration.setHost(dockerConfiguration.getDockerHost());
        configuration.setDockerImage(dockerConfiguration.getDockerImage());
    }

    @Override
    public @Nonnull JPanel getMainPanel() {
        return pnlRoot;
    }

    @Override
    protected @Nonnull JComboBox<Artifact> getCbArtifact() {
        return new ComboBox<>();
    }

    @Override
    protected @Nonnull JLabel getLblArtifact() {
        return new JLabel();
    }

    @Override
    protected @Nonnull JComboBox<MavenProject> getCbMavenProject() {
        return new ComboBox<>();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlConfiguration = new DockerImageConfigurationPanel(this.project);
        this.pnlConfiguration.setHideImageNamePanelForExistingImage(true);
    }

    private void $$$setupUI$$$() {
    }
}
