/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.JBIntSpinner;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerConfiguration;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.containerregistry.dockerhost.DockerHostRunConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class DockerHostRunConfigurationSettingPanel extends AzureSettingPanel<DockerHostRunConfiguration> {
    private JPanel pnlRoot;
    private DockerImageConfigurationPanel pnlConfiguration;
    private JBIntSpinner txtTargetPort;
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
        this.pnlConfiguration.addImageListener(this::onSelectImage);
    }

    private void onSelectImage(DockerImage image) {
        final DockerPushConfiguration value = pnlConfiguration.getValue();
        AzureTaskManager.getInstance().runLater(() ->
                DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(DataManager.getInstance().getDataContext(getMainPanel()), this.runConfiguration, image), AzureTask.Modality.ANY);
        Optional.ofNullable(image).ifPresent(i -> AzureTaskManager.getInstance().runInBackground("Inspecting image", () -> AzureDockerClient.getExposedPorts(value.getDockerHost(), image))
            .thenAccept(ports -> {
                    final Integer port = ports.stream().findFirst().orElse(null);
                    Optional.ofNullable(port).ifPresent(p -> AzureTaskManager.getInstance().runLater(() -> txtTargetPort.setNumber(p), AzureTask.Modality.ANY));
                }));
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
        dockerConfiguration.setDockerHost(data.getDockerHostConfiguration());
        dockerConfiguration.setDockerImage(data.getDockerImageConfiguration());
        pnlConfiguration.setValue(dockerConfiguration);
        Optional.ofNullable(data.getPort()).ifPresent(txtTargetPort::setNumber);
    }

    @Override
    protected void apply(@Nonnull DockerHostRunConfiguration configuration) {
        final DockerConfiguration dockerConfiguration = pnlConfiguration.getValue();
        configuration.setHost(dockerConfiguration.getDockerHost());
        configuration.setDockerImage(dockerConfiguration.getDockerImage());
        configuration.setPort(txtTargetPort.getNumber());
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
        this.txtTargetPort = new JBIntSpinner(80, 1, 65535);
        this.pnlConfiguration = new DockerImageConfigurationPanel(this.project);
        this.pnlConfiguration.setEnableCustomizedImageName(false);
    }

    private void $$$setupUI$$$() {
    }
}
