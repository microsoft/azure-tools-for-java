/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Optional;

public class DockerWebAppSettingPanel extends AzureSettingPanel<WebAppOnLinuxDeployConfiguration> {
    private JPanel pnlRoot;
    private JPanel pnlDockerConfigurationHolder;
    private DockerWebAppComboBox cbWebApp;

    private DockerImageConfigurationPanel pnlDockerConfiguration;

    private final WebAppOnLinuxDeployConfiguration configuration;

    public DockerWebAppSettingPanel(@Nonnull Project project, @Nonnull final WebAppOnLinuxDeployConfiguration configuration) {
        super(project, false);
        this.configuration = configuration;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.pnlDockerConfiguration = new DockerImageConfigurationPanel(project);
        this.pnlDockerConfigurationHolder.add(pnlDockerConfiguration.getPnlRoot(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        this.pnlDockerConfiguration.enableContainerRegistryPanel();
        final AzureFormInput.AzureValueChangeListener<DockerImage> runnable = image -> AzureTaskManager.getInstance().runLater(() ->
                DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(DataManager.getInstance().getDataContext(pnlRoot), this.configuration, image), AzureTask.Modality.ANY);
        this.pnlDockerConfiguration.addImageListener(runnable);
        this.cbWebApp.reloadItems();
    }

    @Override
    public @Nonnull String getPanelName() {
        return "Run On Web App for Containers";
    }

    @Override
    public void disposeEditor() {

    }

    @Override
    protected void resetFromConfig(@Nonnull WebAppOnLinuxDeployConfiguration configuration) {
        if (StringUtils.isAllEmpty(configuration.getWebAppId(), configuration.getAppName())) {
            return;
        }
        cbWebApp.setValue(configuration.getWebAppConfig());
        pnlDockerConfiguration.setValue(configuration.getDockerPushConfiguration());
    }

    @Override
    protected void apply(@Nonnull WebAppOnLinuxDeployConfiguration configuration) {
        final DockerPushConfiguration value = pnlDockerConfiguration.getValue();
        final WebAppConfig webappConfig = cbWebApp.getValue();
        Optional.ofNullable(webappConfig).ifPresent(configuration::setWebAppConfig);
        Optional.ofNullable(value).ifPresent(configuration::setDockerPushConfiguration);
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

    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbWebApp = new DockerWebAppComboBox(project);
    }
}
