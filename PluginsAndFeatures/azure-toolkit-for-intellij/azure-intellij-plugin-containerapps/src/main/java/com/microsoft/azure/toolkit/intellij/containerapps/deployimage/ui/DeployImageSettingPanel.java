/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerapps.deployimage.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ContainerAppComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.IngressConfigurationPanel;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageModel;
import com.microsoft.azure.toolkit.intellij.containerapps.deployimage.DeployImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.containerregistry.buildimage.DockerBuildTaskUtils;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.DockerImageConfigurationPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DeployImageSettingPanel implements AzureFormPanel<DeployImageModel> {
    @Getter
    private JPanel pnlRoot;
    private JPanel pnlDockerPanel;
    private SubscriptionComboBox cbSubscription;
    private ContainerAppComboBox cbContainerApp;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private JLabel lblEnv;
    private IngressConfigurationPanel pnlIngressConfiguration;
    private final Project project;
    private DockerImageConfigurationPanel pnlDockerConfiguration;
    private final DeployImageRunConfiguration configuration;

    public DeployImageSettingPanel(@NotNull Project project, DeployImageRunConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        pnlDockerConfiguration = new DockerImageConfigurationPanel(project);
        pnlDockerPanel.add(pnlDockerConfiguration.getPnlRoot(), new GridConstraints(0, 0, 1, 1, 0, GridConstraints.FILL_BOTH, 3, 3, null, null, null, 0));
        this.pnlDockerConfiguration.enableContainerRegistryPanel();
        this.cbSubscription.addItemListener(this::onSelectSubscription);
        this.cbContainerApp.addItemListener(this::onSelectContainerApp);
        final AzureFormInput.AzureValueChangeListener<DockerImage> runnable = image -> AzureTaskManager.getInstance().runLater(() ->
                DockerBuildTaskUtils.updateDockerBuildBeforeRunTasks(DataManager.getInstance().getDataContext(pnlRoot), this.configuration, image), AzureTask.Modality.ANY);
        this.pnlDockerConfiguration.addImageListener(runnable);
    }

    private void onSelectContainerApp(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof ContainerApp) {
            final ContainerApp containerApp = (ContainerApp) itemEvent.getItem();
            Optional.ofNullable(containerApp.getIngressConfig()).ifPresent(this.pnlIngressConfiguration::setValue);
        }
    }

    private void onSelectSubscription(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbContainerApp.setSubscription(subscription);
        }
    }

    @Override
    public void setValue(final DeployImageModel data) {
        pnlDockerConfiguration.setValue(data);
        Optional.ofNullable(data.getIngressConfig()).ifPresent(pnlIngressConfiguration::setValue);
        Optional.ofNullable(data.getEnvironmentVariables()).ifPresent(inputEnv::setEnvironmentVariables);
        Optional.ofNullable(data.getContainerAppId())
                .map(id -> (ContainerApp) Azure.az(AzureContainerApps.class).getById(id))
                .ifPresent(app -> {
                    cbContainerApp.setValue(app);
                    cbSubscription.setValue(app.getSubscription());
                });
    }

    @Override
    public DeployImageModel getValue() {
        final DeployImageModel model = new DeployImageModel();
        Optional.ofNullable(cbContainerApp.getValue()).map(ContainerApp::getId).ifPresent(model::setContainerAppId);
        Optional.ofNullable(pnlDockerConfiguration.getValue()).map(DockerPushConfiguration::getDockerImage).ifPresent(model::setDockerImage);
        Optional.ofNullable(pnlDockerConfiguration.getValue()).map(DockerPushConfiguration::getDockerHost).ifPresent(model::setDockerHost);
        Optional.ofNullable(pnlDockerConfiguration.getValue()).map(DockerPushConfiguration::getContainerRegistryId).ifPresent(model::setContainerRegistryId);
        Optional.ofNullable(pnlIngressConfiguration.getValue()).ifPresent(model::setIngressConfig);
        Optional.ofNullable(inputEnv.getEnvironmentVariables()).ifPresent(model::setEnvironmentVariables);
        return model;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbContainerApp, pnlDockerConfiguration);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here

    }
}
