/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBoxSimple;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ConnectorDialog<R extends Resource, C extends Resource> extends AzureDialog<Connection<R, C>>
        implements AzureForm<Connection<R, C>> {
    private final Project project;
    private JPanel contentPane;
    private AzureFormJPanel<C> consumerPanel;
    private AzureFormJPanel<R> resourcePanel;
    private AzureComboBox<ResourceDefinition<? extends Resource>> consumerTypeSelector;
    private AzureComboBox<ResourceDefinition<? extends Resource>> resourceTypeSelector;
    private JPanel consumerPanelContainer;
    private JPanel resourcePanelContainer;
    private JBLabel consumerTypeLabel;
    private JBLabel resourceTypeLabel;
    private TitledSeparator resourceTitle;
    private TitledSeparator consumerTitle;

    @Getter
    private final String dialogTitle = "Azure Resource Connector";
    private C consumer;
    private R resource;

    public ConnectorDialog(Project project) {
        super(project);
        this.project = project;
        this.init();
    }

    protected void init() {
        super.init();
        this.setOkActionListener(this::saveConnection);
        this.consumerTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        this.resourceTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        final var consumerDefinitions = ResourceManager.getDefinitions(ResourceDefinition.CONSUMER);
        final var resourceDefinitions = ResourceManager.getDefinitions(ResourceDefinition.RESOURCE);
        if (consumerDefinitions.size() == 1) {
            this.fixConsumerType(consumerDefinitions.get(0));
        }
        // if (resourceDefinitions.size() == 1) {
        //     this.setResourceType(resourceDefinitions.get(0));
        // }
    }

    protected void onResourceOrConsumerTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final ResourceDefinition<? extends Resource> consumerDefinition = this.consumerTypeSelector.getValue();
            final ResourceDefinition<? extends Resource> resourceDefinition = this.resourceTypeSelector.getValue();
            if (Objects.nonNull(consumerDefinition) && Objects.nonNull(resourceDefinition)) {
                final ConnectionDefinition<Resource, Resource> connectionDefinition = ConnectionManager.getDefinition(resourceDefinition.getType(), consumerDefinition.getType());
                final AzureDialog<Connection<Resource, Resource>> dialog = Optional.ofNullable(connectionDefinition)
                        .map(ConnectionDefinition::getConnectorDialog).orElse(null);
                if (Objects.nonNull(dialog)) {
                    dialog.show();
                    return;
                }
            }
            final GridConstraints constraints = new GridConstraints();
            constraints.setFill(GridConstraints.FILL_HORIZONTAL);
            constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
            constraints.setUseParentLayout(true);
            final ResourceDefinition<? extends Resource> definition = (ResourceDefinition<? extends Resource>) e.getItem();
            if (Objects.equals(e.getSource(), this.consumerTypeSelector)) {
                this.consumerPanel = (AzureFormJPanel<C>) consumerDefinition.getResourcesPanel(consumerDefinition.getType(), this.project);
                Optional.ofNullable(this.consumer).ifPresent(c -> this.consumerPanel.setData(c));
                this.consumerPanelContainer.removeAll();
                this.consumerPanelContainer.add(this.consumerPanel.getContentPanel(), constraints);
            } else {
                this.resourcePanel = (AzureFormJPanel<R>) resourceDefinition.getResourcesPanel(resourceDefinition.getType(), this.project);
                Optional.ofNullable(this.resource).ifPresent(c -> this.resourcePanel.setData(c));
                this.resourcePanelContainer.removeAll();
                this.resourcePanelContainer.add(this.resourcePanel.getContentPanel(), constraints);
            }
        }
        this.contentPane.revalidate();
        this.contentPane.repaint();
        this.pack();
        this.centerRelativeToParent();
    }

    protected void saveConnection(Connection<R, C> connection) {
        AzureTaskManager.getInstance().runLater(() -> {
            this.close(0);
            final R resource = connection.getResource();
            final C consumer = connection.getConsumer();
            final ConnectionManager connectionManager = this.project.getService(ConnectionManager.class);
            final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
            final ConnectionDefinition<R, C> definition = ConnectionManager.getDefinitionOrDefault(resource.getType(), consumer.getType());
            if (definition.validate(connection, this.project)) {
                resourceManager.addResource(resource);
                resourceManager.addResource(consumer);
                connectionManager.addConnection(connection);
                final String message = String.format("The connection between %s and %s has been successfully created.",
                        resource.toString(), consumer.toString());
                AzureMessager.getMessager().success(message);
            }
        });
    }

    @Override
    public AzureForm<Connection<R, C>> getForm() {
        return this;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPane;
    }

    @Override
    public Connection<R, C> getData() {
        final R resource = this.resourcePanel.getData();
        final C consumer = this.consumerPanel.getData();
        final ConnectionDefinition<R, C> definition = ConnectionManager.getDefinition(resource.getType(), consumer.getType());
        if (Objects.nonNull(definition)) {
            return definition.create(resource, consumer);
        }
        return new DefaultConnection<>(resource, consumer);
    }

    @Override
    public void setData(Connection<R, C> connection) {
        this.setConsumer(connection.getConsumer());
        this.setResource(connection.getResource());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final ArrayList<AzureFormInput<?>> inputs = new ArrayList<>();
        inputs.addAll(consumerPanel.getInputs());
        inputs.addAll(resourcePanel.getInputs());
        return inputs;
    }

    public void setResource(final R resource) {
        this.resource = resource;
        this.resourceTypeSelector.setValue(new ItemReference<>(resource.getType(), ResourceDefinition::getType), true);
    }

    public void setConsumer(final C consumer) {
        this.consumer = consumer;
        this.consumerTypeSelector.setValue(new ItemReference<>(consumer.getType(), ResourceDefinition::getType), true);
    }

    private void fixResourceType(ResourceDefinition<? extends Resource> definition) {
        this.resourceTitle.setText(definition.getTitle());
        this.resourceTypeLabel.setVisible(false);
        this.resourceTypeSelector.setVisible(false);
    }

    private void fixConsumerType(ResourceDefinition<? extends Resource> definition) {
        this.consumerTitle.setText(String.format("Consumer (%s)", definition.getTitle()));
        this.consumerTypeLabel.setVisible(false);
        this.consumerTypeSelector.setVisible(false);
    }

    private void createUIComponents() {
        this.consumerTypeSelector = new AzureComboBoxSimple<>(() -> ResourceManager.getDefinitions(ResourceDefinition.CONSUMER));
        this.resourceTypeSelector = new AzureComboBoxSimple<>(() -> ResourceManager.getDefinitions(ResourceDefinition.RESOURCE));
    }
}
