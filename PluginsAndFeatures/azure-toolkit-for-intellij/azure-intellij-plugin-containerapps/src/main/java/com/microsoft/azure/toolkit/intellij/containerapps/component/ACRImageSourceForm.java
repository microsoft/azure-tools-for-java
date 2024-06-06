/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.ui.components.ActionLink;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.ACRRegistryComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ACRImageSourceForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    @Getter
    private JPanel contentPanel;

    private ACRRegistryComboBox selectorRegistry;
    private ACRRepositoryComboBox selectorRepository;
    private ACRRepositoryTagComboBox selectorTag;
    private ActionLink linkEnableAdminUser;

    @Setter
    @Getter
    private ContainerApp containerApp;
    public ACRImageSourceForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        final ContainerRegistry registry = Objects.requireNonNull(this.selectorRegistry.getValue(), "'registry' is required.");
        final Repository repository = Objects.requireNonNull(this.selectorRepository.getValue(), "'repository' is required.");
        final Tag tag = Objects.requireNonNull(this.selectorTag.getValue(), "'tag' is required.");
        final String fullImageName = tag.getFullName();
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        config.setContainerRegistry(registry);
        return config;
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        final ContainerRegistry registry = Objects.requireNonNull(config.getContainerRegistry(), "container registry is null.");
        final String f = config.getFullImageName();
        final String repositoryName = f.substring(f.indexOf("/") + 1, f.lastIndexOf(":"));
        this.selectorRegistry.setValue(registry);
        this.selectorRepository.setValue(r -> r.getName().equalsIgnoreCase(repositoryName));
        this.selectorTag.setValue(t -> t.getName().equalsIgnoreCase(config.getTag()));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorRegistry,
            this.selectorRepository,
            this.selectorTag
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void init() {
        this.selectorRegistry.addItemListener(this::onRegistryChanged);
        this.selectorRegistry.addValidator(this::validateAdminUserEnableStatus);
        this.selectorRepository.addItemListener(this::onRepositoryChanged);
        this.selectorRegistry.setRequired(true);
        this.selectorRepository.setRequired(true);
        this.selectorTag.setRequired(true);
        this.linkEnableAdminUser.addActionListener(this::onEnableAdminUser);
    }

    private void onRegistryChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.selectorRepository.setRegistry((ContainerRegistry) e.getItem());
            this.linkEnableAdminUser.setVisible(!((ContainerRegistry) e.getItem()).isAdminUserEnabled());
        }
    }

    private void onRepositoryChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.selectorTag.setRepository(((Repository) e.getItem()));
        }
    }

    private void onEnableAdminUser(ActionEvent actionEvent) {
        final ContainerRegistry value = selectorRegistry.getValue();
        if (Objects.nonNull(value)) {
            AzureTaskManager.getInstance().runInBackground("Enable Admin User", () -> {
                value.enableAdminUser(); // call method instead of action directly as we need to invoke callback actions
                AzureTaskManager.getInstance().runLater(() -> {
                    selectorRegistry.validateValueAsync();
                    selectorRegistry.reloadItems();
                }, AzureTask.Modality.ANY);
            });
        }
    }

    private AzureValidationInfo validateAdminUserEnableStatus() {
        final ContainerRegistry value = selectorRegistry.getValue();
        if (Objects.isNull(value)) {
            return AzureValidationInfo.success(selectorRegistry);
        }
        return value.isAdminUserEnabled() ? AzureValidationInfo.success(selectorRegistry) :
                AzureValidationInfo.error(String.format("Admin user is not enabled for registry (%s)", value.getName()), selectorRegistry);
    }
}
