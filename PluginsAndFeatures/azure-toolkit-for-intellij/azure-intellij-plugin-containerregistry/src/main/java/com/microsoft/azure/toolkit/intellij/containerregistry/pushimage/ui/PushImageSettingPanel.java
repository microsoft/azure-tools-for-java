/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.containerregistry.pushimage.PushImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;
import java.util.Optional;

public class PushImageSettingPanel extends AzureSettingPanel<PushImageRunConfiguration> {
    private final PushImageRunConfiguration runConfiguration;
    private JPanel pnlRoot;
    private DockerImageConfigurationPanel pnlConfiguration;

    public PushImageSettingPanel(@NotNull Project project, PushImageRunConfiguration runConfiguration) {
        super(project, false);
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
    public @NotNull String getPanelName() {
        return "Push Image";
    }

    @Override
    public void disposeEditor() {

    }

    @Override
    protected void resetFromConfig(@NotNull PushImageRunConfiguration configuration) {
        final DockerPushConfiguration result = new DockerPushConfiguration();
        result.setDockerImage(configuration.getDockerImageConfiguration());
        result.setDockerHost(configuration.getDockerHostConfiguration());
        final ContainerRegistry containerRegistry = (ContainerRegistry) Optional.ofNullable(configuration.getContainerRegistryId())
                .map(id -> Azure.az(AzureContainerRegistry.class).getById(id)).orElse(null);
        result.setContainerRegistry(containerRegistry);
        pnlConfiguration.setValue(result);
    }

    @Override
    protected void apply(@NotNull PushImageRunConfiguration configuration) {
        final DockerPushConfiguration value = pnlConfiguration.getValue();
        configuration.setDockerImage(value.getDockerImage());
        configuration.setHost(value.getDockerHost());
        configuration.setContainerRegistry(value.getContainerRegistry());
    }

    @Override
    public @NotNull JPanel getMainPanel() {
        return pnlRoot;
    }

    @Override
    protected @NotNull JComboBox<Artifact> getCbArtifact() {
        return new ComboBox<>();
    }

    @Override
    protected @NotNull JLabel getLblArtifact() {
        return new JLabel();
    }

    @Override
    protected @NotNull JComboBox<MavenProject> getCbMavenProject() {
        return new ComboBox<>();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.pnlConfiguration = new DockerImageConfigurationPanel(project);
        this.pnlConfiguration.enableContainerRegistryPanel();
        this.pnlConfiguration.setHideImageNamePanelForExistingImage(false);
    }

    private void $$$setupUI$$$() {
    }
}
