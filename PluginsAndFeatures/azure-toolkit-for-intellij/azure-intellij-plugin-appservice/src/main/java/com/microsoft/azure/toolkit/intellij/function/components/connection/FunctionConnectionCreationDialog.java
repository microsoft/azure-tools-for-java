/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components.connection;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.function.connection.CommonConnectionResource;
import com.microsoft.azure.toolkit.intellij.function.connection.ConnectionTarget;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FunctionConnectionCreationDialog extends AzureDialog<FunctionConnectionComboBox.ConnectionConfiguration> implements AzureForm<FunctionConnectionComboBox.ConnectionConfiguration> {

    public static final String CONNECTION_CREATED_MESSAGE = "The connection between %s and %s has been successfully created.";
    private JLabel lblConnectionName;
    private JLabel lblConnectionString;
    private AzureTextInput txtConnectionName;
    private AzureTextInput txtConnectionString;
    private JPanel pnlRoot;
    private JPanel pnlResource;
    private JPanel pnlConnectionString;
    private JRadioButton rdoSelectResource;
    private JRadioButton rdoConnectionString;
    private JPanel pnlMode;
    private JPanel descriptionContainer;
    private JTextPane descriptionPane;
    private Subscription subscription;

    private final Project project;
    private final Module module;
    private final FunctionSupported<?> definition;
    @Getter
    private Connection<?,?> connection;
    private AzureFormJPanel<? extends Resource<?>> resourcePanel;

    public FunctionConnectionCreationDialog(final Project project, final Module module, final String resourceType) {
        this(project, module, resourceType, null);
    }

    public FunctionConnectionCreationDialog(final Project project, final Module module, final String resourceType, final String helpId) {
        super(project);
        this.project = project;
        this.module = module;
        this.definition = FunctionDefinitionManager.getFunctionDefinitionByResourceType(resourceType);
        this.helpId = helpId;
        $$$setupUI$$$();
        init();
    }

    protected void init() {
        super.init();
        final ButtonGroup group = new ButtonGroup();
        group.add(rdoSelectResource);
        group.add(rdoConnectionString);

        rdoSelectResource.addItemListener(ignore -> toggleSelectionMode());
        rdoConnectionString.addItemListener(ignore -> toggleSelectionMode());
        rdoSelectResource.setSelected(Objects.nonNull(definition));
        rdoConnectionString.setSelected(!Objects.nonNull(definition));
        descriptionPane.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            protected void hyperlinkActivated(@NotNull HyperlinkEvent e) {
                if (Objects.nonNull(e.getURL())) {
                    AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(e.getURL().toString());
                }
            }
        });
        final Font font = UIManager.getFont("Label.font");
        final Color foregroundColor = UIManager.getColor("Label.foreground");
        descriptionPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        if (font != null && foregroundColor != null) {
            descriptionPane.setFont(font);
            descriptionPane.setForeground(foregroundColor);
        }
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
        pnlResource.setVisible(rdoSelectResource.isSelected());
        pnlConnectionString.setVisible(rdoConnectionString.isSelected());
        pnlMode.setVisible(Objects.nonNull(definition));
        Optional.ofNullable(resourcePanel).ifPresent(p -> p.setRequired(rdoSelectResource.isSelected()));
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
        final String icon = rdoConnectionString.isSelected() || Objects.isNull(definition) ? null : definition.getIcon();
        return new FunctionConnectionComboBox.ConnectionConfiguration(name, null);
    }

    @Override
    public void setValue(FunctionConnectionComboBox.ConnectionConfiguration data) {
        throw new UnsupportedOperationException("not support");
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(txtConnectionName, rdoConnectionString.isSelected() ? txtConnectionString : resourcePanel)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    protected void doOKAction() {
        final Resource resource = getResource();
        final Resource consumer = ModuleResource.Definition.IJ_MODULE.define(module.getName());
        this.connection = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(),
                consumer.getDefinition()).define(resource, consumer);
        this.connection.setEnvPrefix(txtConnectionName.getValue());
        if (this.connection.validate(this.project)) {
            AzureTaskManager.getInstance().write(() -> AzureModule.from(module).initializeWithDefaultProfileIfNot().addConnection(connection).save());
        }
        super.doOKAction();
    }

    private Resource<?> getResource() {
        if (rdoConnectionString.isSelected()) {
            final ConnectionTarget target = ConnectionTarget.builder()
                    .name(txtConnectionName.getValue())
                    .connectionString(txtConnectionString.getValue()).build();
            return CommonConnectionResource.Definition.INSTANCE.define(target);
        } else {
            return Objects.requireNonNull(resourcePanel).getValue();
        }
    }

    public void setOKActionText(@Nonnull final String text) {
        super.setOKButtonText(text);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }

    public void setFixedConnectionName(String azureWebJobStorageKey) {
        txtConnectionName.setValue(azureWebJobStorageKey);
        txtConnectionName.setEnabled(false);
        txtConnectionName.setEditable(false);
        txtConnectionName.setVisible(false);
        lblConnectionName.setVisible(false);
    }

    public void setDescription(@Nonnull final String description, @Nullable Icon icon) {
        descriptionContainer.setVisible(true);
        descriptionPane.setText(description);
    }
}
