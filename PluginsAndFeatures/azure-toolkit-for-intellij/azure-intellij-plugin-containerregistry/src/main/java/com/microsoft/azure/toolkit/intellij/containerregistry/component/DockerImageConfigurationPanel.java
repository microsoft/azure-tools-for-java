/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DockerImageConfigurationPanel implements AzureForm<DockerPushConfiguration> {
    private JPanel pnlRoot;
    private AzureDockerHostComboBox cbDockerHost;
    private AzureDockerImageComboBox cbDockerImage;
    private AzureTextInput txtRepositoryName;
    private JLabel lblRepository;
    private JLabel lblTagName;
    private AzureTextInput txtTagName;
    private AzureContainerRegistryComboBox cbContainerRegistry;
    private JLabel lblRepositoryPrefix;
    private JPanel pnlImageName;
    private JPanel pnlContainerRegistry;

    private final Project project;
    @Getter
    @Setter
    private boolean hideImageNamePanelForExistingImage;

    public DockerImageConfigurationPanel(final Project project) {
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    private void init() {
        this.cbDockerHost.setRequired(true);
        this.cbDockerImage.setRequired(true);
        this.cbContainerRegistry.addItemListener(this::onSelectContainerRegistry);
        this.cbDockerHost.addItemListener(this::onDockerHostChanged);
        this.cbDockerImage.addItemListener(ignore -> updateImageConfigurationUI());
    }

    public void enableContainerRegistryPanel() {
        pnlContainerRegistry.setVisible(true);
        cbContainerRegistry.reloadItems();
    }

    private void onSelectContainerRegistry(ItemEvent itemEvent) {
        final ContainerRegistry value = cbContainerRegistry.getValue();
        Mono.fromCallable(() -> Optional.ofNullable(value).map(r -> r.getLoginServerUrl() + "/").orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(prefix -> AzureTaskManager.getInstance().runLater(() -> {
                    lblRepositoryPrefix.setText(prefix);
                    lblRepositoryPrefix.setVisible(StringUtils.isNotBlank(prefix));
                }, AzureTask.Modality.ANY));
    }

    private void updateImageConfigurationUI() {
        final DockerImage image = this.cbDockerImage.getValue();
        final boolean isDraftImage = Optional.ofNullable(image).map(DockerImage::isDraft).orElse(false);
        this.txtRepositoryName.setEnabled(isDraftImage);
        this.txtRepositoryName.setRequired(isDraftImage);
        this.txtRepositoryName.revalidate();
        Optional.ofNullable(image).map(DockerImage::getRepositoryName).ifPresent(this.txtRepositoryName::setValue);
        this.lblRepository.setEnabled(isDraftImage);
        this.txtTagName.setEnabled(isDraftImage);
        this.txtTagName.setRequired(isDraftImage);
        this.txtTagName.revalidate();
        this.lblTagName.setEnabled(isDraftImage);
        Optional.ofNullable(image).map(DockerImage::getTagName).ifPresent(this.txtTagName::setValue);
        pnlImageName.setVisible(!hideImageNamePanelForExistingImage || isDraftImage);
    }

    private void onDockerHostChanged(@Nullable final ItemEvent event) {
        this.cbDockerImage.setDockerHost(cbDockerHost.getValue());
    }

    @Override
    public DockerPushConfiguration getValue() {
        final DockerPushConfiguration result = new DockerPushConfiguration();
        result.setDockerHost(this.cbDockerHost.getValue());
        final DockerImage image = this.cbDockerImage.getValue();
        result.setDockerImage(image);
        if (Optional.ofNullable(image).map(DockerImage::isDraft).orElse(false)) {
            image.setRepositoryName(this.txtRepositoryName.getValue());
            image.setTagName(this.txtTagName.getValue());
        }
        result.setContainerRegistry(cbContainerRegistry.getValue());
        return result;
    }

    @Override
    public void setValue(@Nonnull final DockerPushConfiguration data) {
        Optional.ofNullable(data.getDockerHost()).ifPresent(cbDockerHost::setValue);
        Optional.ofNullable(data.getDockerImage()).ifPresent(image -> {
            this.cbDockerImage.setValue(new AzureComboBox.ItemReference<>(i -> (!image.isDraft() && StringUtils.equals(i.getImageName(), image.getImageName())) ||
                    (image.isDraft() && Objects.equals(i.getDockerFile(), image.getDockerFile()))));
            if (image.isDraft()) {
                this.txtRepositoryName.setValue(image.getRepositoryName());
                this.txtTagName.setValue(image.getTagName());
            }
        });
        Optional.ofNullable(data.getContainerRegistry()).ifPresent(cbContainerRegistry::setValue);
        updateImageConfigurationUI();
    }

    public void addImageListener(@Nonnull final AzureValueChangeListener<DockerImage> listener) {
        this.cbDockerImage.addValueChangedListener(listener);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbDockerHost, cbDockerImage);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbDockerHost = new AzureDockerHostComboBox(project);
        this.cbDockerImage = new AzureDockerImageComboBox(project);
        this.cbContainerRegistry = new AzureContainerRegistryComboBox(true);
    }

    private void $$$setupUI$$$() {
    }
}
