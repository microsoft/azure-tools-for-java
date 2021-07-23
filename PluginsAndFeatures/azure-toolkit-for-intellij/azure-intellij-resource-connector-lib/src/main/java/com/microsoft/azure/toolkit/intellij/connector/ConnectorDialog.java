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
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
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
    @Setter
    private String resourceType;
    @Setter
    private String consumerType;

    public ConnectorDialog(Project project) {
        super(project);
        this.project = project;
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.setOkActionListener(this::saveConnection);
        this.consumerTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        this.resourceTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        final var consumerDefinitions = ResourceManager.getDefinitions(ResourceDefinition.CONSUMER);
        if (consumerDefinitions.size() == 1) {
            this.fixConsumerType(consumerDefinitions.get(0));
        }
    }

    protected void onResourceOrConsumerTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final ResourceDefinition<? extends Resource> consumerDefinition = this.consumerTypeSelector.getValue();
            final ResourceDefinition<? extends Resource> resourceDefinition = this.resourceTypeSelector.getValue();
            if (Objects.nonNull(consumerDefinition) && Objects.nonNull(resourceDefinition)) {
                final ConnectionDefinition<Resource, Resource> connectionDefinition =
                        ConnectionManager.getDefinition(resourceDefinition.getType(), consumerDefinition.getType());
                final AzureDialog<Connection<Resource, Resource>> dialog = Optional.ofNullable(connectionDefinition)
                        .map(ConnectionDefinition::getConnectorDialog).orElse(null);
                if (Objects.nonNull(dialog)) {
                    dialog.show();
                    return;
                }
            }
            if (Objects.equals(e.getSource(), this.consumerTypeSelector)) {
                this.consumerPanel = this.updatePanel(this.consumerTypeSelector.getValue(), this.consumerPanelContainer, this.consumer);
            } else {
                this.resourcePanel = this.updatePanel(this.resourceTypeSelector.getValue(), this.resourcePanelContainer, this.resource);
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
            final R connectionResource = connection.getResource();
            final C connectionConsumer = connection.getConsumer();
            final ConnectionManager connectionManager = this.project.getService(ConnectionManager.class);
            final ResourceManager resourceManager = ServiceManager.getService(ResourceManager.class);
            final ConnectionDefinition<R, C> definition = ConnectionManager.getDefinitionOrDefault(connectionResource.getType(), connectionConsumer.getType());
            if (definition.validate(connection, this.project)) {
                resourceManager.addResource(connectionResource);
                resourceManager.addResource(connectionConsumer);
                connectionManager.addConnection(connection);
                final String message = String.format("The connection between %s and %s has been successfully created.",
                        connectionResource.toString(), connectionConsumer.toString());
                AzureMessager.getMessager().success(message);
                // send connect successful event.
                project.getMessageBus().syncPublisher(AzureResourceConnectorBusNotifier.AZURE_RESOURCE_CONNECTOR_TOPIC).afterAction(connection);
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
        final R localResource = this.resourcePanel.getData();
        final C localConsumer = this.consumerPanel.getData();
        final ConnectionDefinition<R, C> definition = ConnectionManager.getDefinition(localResource.getType(), localConsumer.getType());
        if (Objects.nonNull(definition)) {
            return definition.create(localResource, localConsumer);
        }
        return new DefaultConnection<>(localResource, localConsumer);
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

    @Override
    @SuppressWarnings({"unchecked"})
    public void show() {
        // initialize resource panel
        final String resourceTypeTemp = ObjectUtils.firstNonNull(Optional.ofNullable(this.resource).map(Resource::getType).orElse(null), this.resourceType,
                ResourceManager.getDefinitions(ResourceDefinition.RESOURCE).stream().map(ResourceDefinition::getType).findFirst().orElse(null));
        Optional.ofNullable(ResourceManager.getDefinition(resourceTypeTemp))
                .ifPresent(definition -> this.resourcePanel = this.updatePanel(definition, this.resourcePanelContainer, this.resource));
        // initialize consumer panel
        final String consumerTypeTemp = ObjectUtils.firstNonNull(Optional.ofNullable(this.consumer).map(Resource::getType).orElse(null), this.consumerType,
                ResourceManager.getDefinitions(ResourceDefinition.CONSUMER).stream().map(ResourceDefinition::getType).findFirst().orElse(null));
        Optional.ofNullable(ResourceManager.getDefinition(consumerTypeTemp))
                .ifPresent(definition -> this.consumerPanel = this.updatePanel(definition, this.consumerPanelContainer, this.consumer));
        // call original super method
        super.show();
    }

    public void setResource(final R resource) {
        this.resource = resource;
        this.resourceTypeSelector.setValue(new ItemReference<>(resource.getType(), ResourceDefinition::getType), true);
    }

    public void setConsumer(final C consumer) {
        this.consumer = consumer;
        this.consumerTypeSelector.setValue(new ItemReference<>(consumer.getType(), ResourceDefinition::getType), true);
    }

    private AzureFormJPanel updatePanel(ResourceDefinition<? extends Resource> definition, JPanel resourcePanelContainer, Resource resource) {
        final GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        constraints.setUseParentLayout(true);
        AzureFormJPanel newResourcePanel = definition.getResourcesPanel(definition.getType(), this.project);
        Optional.ofNullable(resource).ifPresent(newResourcePanel::setData);
        resourcePanelContainer.removeAll();
        resourcePanelContainer.add(newResourcePanel.getContentPanel(), constraints);
        return newResourcePanel;
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
