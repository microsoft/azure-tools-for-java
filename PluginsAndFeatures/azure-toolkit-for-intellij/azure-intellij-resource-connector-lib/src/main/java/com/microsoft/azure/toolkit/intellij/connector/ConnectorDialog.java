/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.Future;

import static com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition.CONSUMER;
import static com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition.RESOURCE;

public class ConnectorDialog extends AzureDialog<Connection<?, ?>> implements AzureForm<Connection<?, ?>> {
    public static final String NOT_SIGNIN_TIPS = "<html><a href=\"\">Sign in</a> to select an existing Azure resource.</html>";
    private final Project project;
    private JPanel contentPane;
    @SuppressWarnings("rawtypes")
    private AzureFormJPanel consumerPanel;
    @SuppressWarnings("rawtypes")
    private AzureFormJPanel resourcePanel;
    private AzureComboBox<ResourceDefinition<?>> consumerTypeSelector;
    private AzureComboBox<ResourceDefinition<?>> resourceTypeSelector;
    private JPanel consumerPanelContainer;
    private JPanel resourcePanelContainer;
    private JBLabel consumerTypeLabel;
    private JBLabel resourceTypeLabel;
    private TitledSeparator resourceTitle;
    private TitledSeparator consumerTitle;
    protected JTextField envPrefixTextField;
    private JPanel descriptionContainer;
    private JTextPane descriptionPane;
    private JPanel pnlEnvPrefix;
    private JLabel lblEnvPrefix;
    private TitledSeparator titleAuthentication;
    private JPanel pnlAuthentication;
    private JPanel pnlUserAssignedManagedIdentity;
    private AzureComboBox<AuthenticationType> cbAuthenticationType;
    private UserAssignedManagedIdentityComboBox cbIdentity;
    private SignInHyperLinkLabel signInHyperLinkLabel1;
    private ResourceDefinition<?> resourceDefinition;
    private ResourceDefinition<?> consumerDefinition;

    private Connection<?, ?> connection;
    @Getter

    private Future<?> future;

    @Getter
    private final String dialogTitle = "Azure Resource Connector";

    public ConnectorDialog(Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.contentPane.setPreferredSize(new Dimension(600, -1));
        this.contentPane.setMaximumSize(new Dimension(600, -1));
        final Action.Id<Connection<?, ?>> actionId = Action.Id.of("user/connector.create_or_update_connection.consumer|resource");
        this.setOkAction(new Action<>(actionId)
            .withLabel("Save")
            .withIdParam(c -> c.consumer.getName())
            .withIdParam(c -> c.resource.getName())
            .withSource(c -> c.resource.getData())
            .withAuthRequired(false)
            .withHandler(c -> {
                if (c != null && c.validate(this.project)) {
                    saveConnectionToDotAzure(c);
                }
            }));
        this.consumerTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        this.resourceTypeSelector.addItemListener(this::onResourceOrConsumerTypeChanged);
        this.cbAuthenticationType.addItemListener(this::onAuthenticationTypeChanged);
        final Font font = UIManager.getFont("Label.font");
        final Color foregroundColor = UIManager.getColor("Label.foreground");
        final Color backgroundColor = UIManager.getColor("Label.backgroundColor");
        this.descriptionPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        if (font != null && foregroundColor != null) {
            this.descriptionPane.setFont(font);
            this.descriptionPane.setForeground(foregroundColor);
            this.descriptionPane.setBackground(backgroundColor);
        }

        final var resourceDefinitions = ResourceManager.getDefinitions(RESOURCE);
        final var consumerDefinitions = ResourceManager.getDefinitions(CONSUMER);
        if (resourceDefinitions.size() == 1) {
            this.fixResourceType(resourceDefinitions.get(0));
        }
        if (consumerDefinitions.size() == 1) {
            this.fixConsumerType(consumerDefinitions.get(0));
        }
    }

    private void onSelectResource(Object o) {
        final AzureServiceResource<?> value = o instanceof AzureServiceResource ? (AzureServiceResource<?>) this.resourcePanel.getValue() : null;
        if (Objects.nonNull(value)) {
            cbIdentity.setResource(value);
        }
    }

    private void onAuthenticationTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AuthenticationType authenticationType = this.cbAuthenticationType.getValue();
            this.pnlUserAssignedManagedIdentity.setVisible(authenticationType == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY);
            if (authenticationType == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
                final Resource<?> value = (Resource<?>) this.resourcePanel.getValue();
                if (value instanceof AzResource azResource) {
                    cbIdentity.setSubscription(azResource.getSubscription());
                }
            }
        }
    }

    protected void onResourceOrConsumerTypeChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && !tryOpenCustomDialog()) {
            if (Objects.equals(e.getSource(), this.consumerTypeSelector)) {
                this.setConsumerDefinition(this.consumerTypeSelector.getValue());
            } else {
                this.setResourceDefinition(this.resourceTypeSelector.getValue());
            }
        }
        this.contentPane.revalidate();
        this.contentPane.repaint();
        this.pack();
        this.centerRelativeToParent();
    }

    private boolean tryOpenCustomDialog() {
        final ResourceDefinition<?> cd = this.consumerTypeSelector.getValue();
        final ResourceDefinition<?> rd = this.resourceTypeSelector.getValue();
        if (Objects.nonNull(cd) && Objects.nonNull(rd)) {
            final ConnectionDefinition<?, ?> definition = ConnectionManager.getDefinitionOrDefault(rd, cd);
            final AzureDialog<? extends Connection<?, ?>> dialog = definition.getConnectorDialog();
            if (Objects.nonNull(dialog)) {
                dialog.show();
                return true;
            }
        }
        return false;
    }

    private void saveConnectionToDotAzure(Connection<?, ?> connection) {
        final Resource<?> consumer = connection.getConsumer();
        if (consumer instanceof ModuleResource) {
            final ModuleManager moduleManager = ModuleManager.getInstance(project);
            final Module m = moduleManager.findModuleByName(consumer.getName());
            if (Objects.nonNull(m)) {
                final AzureModule module = AzureModule.from(m);
                final AzureTaskManager taskManager = AzureTaskManager.getInstance();
                taskManager.runLater(() -> taskManager.write(() -> {
                    final Profile profile = module.initializeWithDefaultProfileIfNot();
                    this.future = profile.createOrUpdateConnection(connection);
                    profile.save();
                }));
            }
        }
    }

    @Override
    public AzureForm<Connection<?, ?>> getForm() {
        return this;
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return this.contentPane;
    }

    @Nullable
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Connection<?, ?> getValue() {
        final Resource resource = (Resource<?>) this.resourcePanel.getValue();
        final Resource consumer = (Resource<?>) this.consumerPanel.getValue();
        if (Objects.isNull(resource) || Objects.isNull(consumer)) {
            return null;
        }
        final ConnectionDefinition<?, ?> connectionDefinition = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(), consumer.getDefinition());
        final Connection connection;
        if (Objects.isNull(this.connection)) {
            connection = connectionDefinition.define(resource, consumer);
        } else {
            connection = this.connection;
            connection.setResource(resource);
            connection.setConsumer(consumer);
            connection.setDefinition(connectionDefinition);
        }
        if (resource.getDefinition().isEnvPrefixSupported()) {
            connection.setEnvPrefix(this.envPrefixTextField.getText().trim());
        }
        connection.setAuthenticationType(cbAuthenticationType.getValue());
        if (cbAuthenticationType.getValue() == AuthenticationType.USER_ASSIGNED_MANAGED_IDENTITY) {
            Optional.ofNullable(cbIdentity.getValue()).map(IdentityResource.Definition.INSTANCE::define).ifPresent(connection::setUserAssignedManagedIdentity);
        }
        return connection;
    }

    @Override
    public void setValue(Connection<?, ?> connection) {
        this.setConsumer(connection.getConsumer());
        this.setResource(connection.getResource());
        this.envPrefixTextField.setText(connection.getEnvPrefix());
        this.connection = connection;
        // authentication
        this.cbAuthenticationType.setValue(ObjectUtils.firstNonNull(connection.getAuthenticationType(), AuthenticationType.CONNECTION_STRING));
        Optional.ofNullable(connection.getUserAssignedManagedIdentity()).map(Resource::getData).ifPresent(cbIdentity::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final List<AzureFormInput<?>> inputs = new ArrayList<>();
        //noinspection unchecked
        Optional.ofNullable(resourcePanel).ifPresent(p -> inputs.addAll(p.getInputs()));
        //noinspection unchecked
        Optional.ofNullable(consumerPanel).ifPresent(p -> inputs.addAll(p.getInputs()));
        return inputs;
    }

    public void setResource(@Nullable final Resource<?> resource) {
        if (Objects.nonNull(resource)) {
            this.setResourceDefinition(resource.getDefinition());
            if (resource.isValidResource()) {
                //noinspection unchecked
                this.resourcePanel.setValue(resource);
            }
        } else {
            ResourceManager.getDefinitions(RESOURCE).stream().findFirst().ifPresent(this::setResourceDefinition);
        }
    }

    public void setConsumer(@Nullable final Resource<?> consumer) {
        if (Objects.nonNull(consumer)) {
            this.setConsumerDefinition(consumer.getDefinition());
            //noinspection unchecked
            this.consumerPanel.setValue(consumer);
        } else {
            ResourceManager.getDefinitions(CONSUMER).stream().findFirst().ifPresent(this::setConsumerDefinition);
        }
    }

    public void setResourceDefinition(@Nonnull ResourceDefinition<?> definition) {
        if (!definition.equals(this.resourceDefinition) || Objects.isNull(this.resourcePanel)) {
            this.resourceDefinition = definition;
            this.envPrefixTextField.setText(definition.getDefaultEnvPrefix());
            this.resourceTypeSelector.setValue(new ItemReference<>(definition.getName(), ResourceDefinition::getName));
            this.resourcePanel = this.updatePanel(definition, this.resourcePanelContainer);
            Optional.ofNullable(this.resourcePanel).ifPresent(panel -> panel.addValueChangedListener(this::onSelectResource));

            this.lblEnvPrefix.setVisible(resourceDefinition.isEnvPrefixSupported());
            this.envPrefixTextField.setVisible(resourceDefinition.isEnvPrefixSupported());

            final List<AuthenticationType> supportedAuthenticationTypes = definition.getSupportedAuthenticationTypes();
            this.pnlAuthentication.setVisible(supportedAuthenticationTypes.size() > 1);
            this.cbAuthenticationType.clear();
            this.cbAuthenticationType.setItemsLoader(() -> supportedAuthenticationTypes);
            this.cbAuthenticationType.reloadItems();
        }
    }

    public void setConsumerDefinition(@Nonnull ResourceDefinition<?> definition) {
        if (!definition.equals(this.consumerDefinition) || Objects.isNull(this.consumerPanel)) {
            this.consumerDefinition = definition;
            this.consumerTypeSelector.setValue(new ItemReference<>(definition.getName(), ResourceDefinition::getName));
            this.consumerPanel = this.updatePanel(definition, this.consumerPanelContainer);
        }
    }

    private AzureFormJPanel<?> updatePanel(ResourceDefinition<?> definition, JPanel container) {
        final GridConstraints constraints = new GridConstraints();
        constraints.setFill(GridConstraints.FILL_BOTH);
        constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_WANT_GROW);
        constraints.setUseParentLayout(true);
        final AzureFormJPanel<?> newResourcePanel = definition.getResourcePanel(this.project);
        container.removeAll();
        container.add(newResourcePanel.getContentPanel(), constraints);
        return newResourcePanel;
    }

    private void fixResourceType(ResourceDefinition<?> definition) {
        this.resourceTitle.setText(definition.getTitle());
        this.resourceTypeSelector.setEnabled(false);
        this.resourceTypeSelector.setEditable(false);
    }

    private void fixConsumerType(ResourceDefinition<?> definition) {
        this.consumerTitle.setText(String.format("Consumer (%s)", definition.getTitle()));
        this.consumerTypeLabel.setVisible(false);
        this.consumerTypeSelector.setVisible(false);
    }

    private void createUIComponents() {
        this.consumerTypeSelector = new AzureComboBox<>(() -> ResourceManager.getDefinitions(CONSUMER)) {
            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
        this.resourceTypeSelector = new AzureComboBox<>(() -> ResourceManager.getDefinitions(RESOURCE)) {
            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }
        };
        this.cbIdentity = new UserAssignedManagedIdentityComboBox();
        this.cbAuthenticationType = new AzureComboBox<>() {
            @Nullable
            @Override
            protected AuthenticationType doGetDefaultValue() {
                return AuthenticationType.SYSTEM_ASSIGNED_MANAGED_IDENTITY;
            }

            @Nonnull
            @Override
            protected List<ExtendableTextComponent.Extension> getExtensions() {
                return Collections.emptyList();
            }

            @Override
            protected String getItemText(Object item) {
                return item instanceof AuthenticationType type ? type.getDisplayName() : super.getItemText(item);
            }
        };
    }

    public void setDescription(@Nonnull final String description) {
        descriptionContainer.setVisible(true);
        descriptionPane.setText(description);
    }

    public void setFixedConnectionDefinition(ConnectionDefinition<?, ?> definition) {
        this.fixResourceType(definition.getResourceDefinition());
        this.fixConsumerType(definition.getConsumerDefinition());
    }

    public void setEnvPrefix(@Nonnull final String envPrefix) {
        envPrefixTextField.setText(envPrefix);
    }

    public void setFixedEnvPrefix(@Nonnull final String envPrefix) {
        envPrefixTextField.setText(envPrefix);
        envPrefixTextField.setEnabled(false);
        envPrefixTextField.setEditable(false);
    }

    private void signInAndReloadItems(@Nonnull final HyperlinkLabel notSignInTips) {
        AzureActionManager.getInstance().getAction(Action.REQUIRE_AUTH).handle((a) -> notSignInTips.setVisible(false));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
