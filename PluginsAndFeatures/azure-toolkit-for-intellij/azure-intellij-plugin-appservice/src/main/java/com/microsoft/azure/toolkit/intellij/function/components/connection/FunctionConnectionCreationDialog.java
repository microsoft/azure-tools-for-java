/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components.connection;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionTopics;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.function.connection.CommonConnectionResource;
import com.microsoft.azure.toolkit.intellij.function.connection.ConnectionTarget;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionConnectionCreationDialog extends AzureDialog<FunctionConnectionComboBox.ConnectionConfiguration> implements AzureForm<FunctionConnectionComboBox.ConnectionConfiguration> {

    private JCheckBox chkConnectionString;
    private JLabel lblConnectionName;
    private JLabel lblConnectionString;
    private AzureTextInput txtConnectionName;
    private AzureTextInput txtConnectionString;
    private JPanel pnlRoot;
    private JPanel pnlResource;
    private JPanel pnlConnectionString;
    private Subscription subscription;

    private final Project project;
    private final Module module;
    private final String resourceType;
    private final FunctionSupported<?> definition;
    private AzureFormJPanel<? extends Resource<?>> resourcePanel;

    public FunctionConnectionCreationDialog(final Project project, final Module module, final String resourceType) {
        super(project);
        this.project = project;
        this.module = module;
        this.resourceType = resourceType;
        this.definition = FunctionDefinitionManager.getFunctionDefinitionByResourceType(resourceType);
        $$$setupUI$$$();
        init();
    }

    protected void init() {
        super.init();
        chkConnectionString.addItemListener(ignore -> toggleSelectionMode());
        chkConnectionString.setSelected(Objects.isNull(definition));
        chkConnectionString.setVisible(Objects.nonNull(definition));
        if (Objects.nonNull(definition)) {
            initResourceSelectionPanel();
        }
        toggleSelectionMode();
    }

    private void initResourceSelectionPanel() {
        resourcePanel = definition.getResourcePanel(project);
        this.pnlResource.setLayout(new GridLayoutManager(1, 1));
        final GridConstraints constraints = new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
        this.pnlResource.add(resourcePanel.getContentPanel(), constraints);
    }

    private void toggleSelectionMode() {
        pnlResource.setVisible(!chkConnectionString.isSelected());
        pnlConnectionString.setVisible(chkConnectionString.isSelected());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public AzureForm<FunctionConnectionComboBox.ConnectionConfiguration> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create new Function App connection";
    }

    @Override
    public FunctionConnectionComboBox.ConnectionConfiguration getValue() {
        final String name = txtConnectionName.getValue();
        final String icon = chkConnectionString.isSelected() || Objects.isNull(definition) ? null : definition.getIcon();
        return new FunctionConnectionComboBox.ConnectionConfiguration(name, null);
    }

    @Override
    public void setValue(FunctionConnectionComboBox.ConnectionConfiguration data) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(txtConnectionName, txtConnectionString, resourcePanel).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    protected void doOKAction() {
        final Resource resource = getResource();
        final Resource consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        final Connection<?, ?> connection = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(),
                consumer.getDefinition()).define(resource, consumer);
        connection.setEnvPrefix(txtConnectionName.getValue());
        final ConnectionManager connectionManager = this.project.getService(ConnectionManager.class);
        final ResourceManager resourceManager = ApplicationManager.getApplication().getService(ResourceManager.class);
        if (connection.validate(this.project)) {
            resourceManager.addResource(resource);
            resourceManager.addResource(consumer);
            connectionManager.addConnection(connection);
            final String message = String.format("The connection between %s and %s has been successfully created.",
                    resource.getName(), consumer.getName());
            AzureMessager.getMessager().success(message);
            project.getMessageBus().syncPublisher(ConnectionTopics.CONNECTION_CHANGED).connectionChanged(project, connection, ConnectionTopics.Action.ADD);
        }
        super.doOKAction();
    }

    private Resource<?> getResource() {
        if (chkConnectionString.isSelected()) {
            final ConnectionTarget target = ConnectionTarget.builder()
                    .name(txtConnectionName.getValue())
                    .connectionString(txtConnectionString.getValue()).build();
            return CommonConnectionResource.Definition.INSTANCE.define(target);
        } else {
            return Objects.requireNonNull(resourcePanel).getValue();
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
