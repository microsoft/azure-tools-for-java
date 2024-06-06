/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.JBIntSpinner;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.WebAppOnLinuxDeployConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
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
    private JLabel lblWebApp;
    private JBIntSpinner txtTargetPort;

    private DockerImageConfigurationPanel pnlDockerConfiguration;

    private final WebAppOnLinuxDeployConfiguration configuration;

    public DockerWebAppSettingPanel(@Nonnull Project project, @Nonnull final WebAppOnLinuxDeployConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.pnlDockerConfiguration = new DockerImageConfigurationPanel(project);
        this.pnlDockerConfigurationHolder.add(pnlDockerConfiguration.getPnlRoot(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        this.pnlDockerConfiguration.enableContainerRegistryPanel();
        this.pnlDockerConfiguration.addImageListener(this::onSelectImage);
        this.cbWebApp.reloadItems();
    }

    private void onSelectImage(DockerImage image) {
        final DockerPushConfiguration value = pnlDockerConfiguration.getValue();
        AzureTaskManager.getInstance().runLater(() ->
                DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(DataManager.getInstance().getDataContext(getMainPanel()), this.configuration, image), AzureTask.Modality.ANY);
        Optional.ofNullable(image).ifPresent(i -> AzureTaskManager.getInstance().runInBackground("Inspecting image", () -> AzureDockerClient.getExposedPorts(value.getDockerHost(), image))
            .thenAccept(ports -> {
                    final Integer port = ports.stream().findFirst().orElse(null);
                    Optional.ofNullable(port).ifPresent(p -> AzureTaskManager.getInstance().runLater(() -> txtTargetPort.setNumber(p), AzureTask.Modality.ANY));
                }));
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
        final AppServiceConfig config = configuration.getAppServiceConfig();
        if (StringUtils.isBlank(config.getAppName())) {
            return;
        }
        if (Azure.az(AzureAccount.class).account().getSubscriptions().stream().noneMatch(s -> s.getId().equals(config.subscriptionId()))) {
            cbWebApp.setValue((AppServiceConfig) null);
            return;
        }
        cbWebApp.setConfigModel(config);
        cbWebApp.setValue(c -> AppServiceComboBox.isSameApp(c, config));
        pnlDockerConfiguration.setValue(configuration.getDockerPushConfiguration());
        Optional.ofNullable(configuration.getPort()).ifPresent(txtTargetPort::setNumber);
    }

    @Override
    protected void apply(@Nonnull WebAppOnLinuxDeployConfiguration configuration) {
        final DockerPushConfiguration value = pnlDockerConfiguration.getValue();
        final AppServiceConfig webappConfig = cbWebApp.getValue();
        Optional.ofNullable(webappConfig).ifPresent(configuration::setWebAppConfig);
        Optional.ofNullable(value).ifPresent(configuration::setDockerPushConfiguration);
        Optional.of(txtTargetPort.getNumber()).ifPresent(configuration::setPort);
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
        this.txtTargetPort = new JBIntSpinner(80, 1, 65535);
    }
}
