/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.function.Consumer;

public class ImageForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    private JPanel pnlRoot;
    private ImageSourceTypeComboBox selectorRegistryType;
    private JPanel formImageContainer;
    private JLabel lblRegistryType;
    private DeploymentSourceForm formContent;
    private final List<AzureValueChangeListener<ContainerAppDraft.ImageConfig>> listeners = new ArrayList<>();
    @Setter
    private Consumer<String> onImageFormChanged = type -> {
    };
    @Getter
    private ContainerApp containerApp;

    public ImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorRegistryType.addItemListener(this::onRegistryTypeChanged);
        final String defaultType = ImageSourceTypeComboBox.QUICK_START;
        this.selectorRegistryType.setSelectedItem(defaultType);
        this.updateContentForm(defaultType);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    @Override
    public void setValue(ContainerAppDraft.ImageConfig imageConfig) {
        final ContainerRegistry registry = imageConfig.getContainerRegistry();
        final String type;
        if (registry != null) {
            type = ImageSourceTypeComboBox.ACR;
        } else if (imageConfig.getFullImageName().startsWith("docker.io")) {
            type = ImageSourceTypeComboBox.DOCKER_HUB;
        } else if (imageConfig.getFullImageName().equalsIgnoreCase(QuickStartImageForm.QUICK_START_IMAGE)) {
            type = ImageSourceTypeComboBox.QUICK_START;
        } else {
            type = ImageSourceTypeComboBox.OTHER;
        }
        this.formContent = this.updateContentForm(type);
        this.selectorRegistryType.setSelectedItem(type);
        this.formContent.setValue(imageConfig);
    }

    private void onRegistryTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            this.formContent = this.updateContentForm((String) e.getItem());
        }
    }

    public String getRegistryType() {
        return (String) this.selectorRegistryType.getSelectedItem();
    }

    private synchronized DeploymentSourceForm updateContentForm(String type) {
        final GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        constraints.setUseParentLayout(true);
        final DeploymentSourceForm newFormImage = switch (type) {
            case ImageSourceTypeComboBox.QUICK_START -> new QuickStartImageForm();
            case ImageSourceTypeComboBox.ACR -> new ACRImageSourceForm();
            case ImageSourceTypeComboBox.DOCKER_HUB -> new DockerHubImageForm();
            case ImageSourceTypeComboBox.OTHER -> new OtherPublicRegistryImageForm();
            default -> throw new IllegalArgumentException("Unsupported registry type: " + type);
        };
        this.formImageContainer.removeAll();
        this.formImageContainer.add(newFormImage.getContentPanel(), constraints);
        this.formImageContainer.revalidate();
        this.formImageContainer.repaint();
        this.formContent = newFormImage;
        this.onImageFormChanged.accept(type);
        this.listeners.forEach(this::addValueChangeListenerToAllComponents);
        this.formContent.setContainerApp(this.containerApp);
        return newFormImage;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.formContent);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        return Optional.ofNullable(formContent).map(AzureFormPanel::getValue).orElse(null);
    }

    public void setEnabled(boolean enabled) {
        lblRegistryType.setEnabled(enabled);
        selectorRegistryType.setEnabled(enabled);
        Optional.ofNullable(formContent).map(AzureFormJPanel::getContentPanel).ifPresent(panel -> setEnableForComponent(panel, enabled));
    }

    private void setEnableForComponent(@Nonnull final Component component, final boolean enable) {
        component.setEnabled(enable);
        if (component instanceof Container) {
            for (final Component c : ((Container) component).getComponents()) {
                setEnableForComponent(c, enable);
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        Optional.ofNullable(getContentPanel()).ifPresent(panel -> panel.setVisible(visible));
    }

    public void addValueChangeListenerToAllComponents(final AzureValueChangeListener<ContainerAppDraft.ImageConfig> listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        final Queue<AzureFormInput<?>> inputs = new ArrayDeque<>();
        inputs.add(formContent);
        while (!inputs.isEmpty()) {
            final AzureFormInput<?> input = inputs.poll();
            input.addValueChangedListener(ignore -> listener.accept(this.getValue()));
            if (input instanceof AzureForm<?>) {
                inputs.addAll(((AzureForm<?>) input).getInputs());
            }
        }
    }

    @Override
    public void setContainerApp(final ContainerApp containerApp) {
        this.containerApp = containerApp;
        Optional.ofNullable(formContent).ifPresent(imageForm -> imageForm.setContainerApp(containerApp));
    }
}
