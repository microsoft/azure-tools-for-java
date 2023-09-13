/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.InvalidResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.database.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.TestConnectionActionPanel;
import com.microsoft.azure.toolkit.intellij.database.component.UsernameComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.*;

public abstract class SqlDatabaseResourcePanel<T extends IDatabase> implements AzureFormJPanel<Resource<T>> {
    private final SqlDatabaseResourceDefinition<T> definition;
    private final TailingDebouncer debouncer;
    private JPanel contentPanel;
    private SubscriptionComboBox subscriptionComboBox;
    private ServerComboBox<IDatabaseServer<T>> serverComboBox;
    private DatabaseComboBox<T> databaseComboBox;
    private UsernameComboBox usernameComboBox;
    private JPasswordField inputPasswordField;
    private JTextField urlTextField;
    private JButton testConnectionButton;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JTextPane testResultTextPane;

    private JdbcUrl jdbcUrl;

    public SqlDatabaseResourcePanel(final SqlDatabaseResourceDefinition<T> definition) {
        super();
        this.definition = definition;
        init();
        this.debouncer = new TailingDebouncer(this::onUrlEdited, 500);
        initListeners();
    }

    @Override
    public JPanel getContentPanel() {
        return contentPanel;
    }

    private void init() {
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testResultTextPane.setVisible(false);
        testConnectionButton.setEnabled(Objects.nonNull(this.jdbcUrl));
        // username loader
        this.usernameComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ? Collections.emptyList() :
            Collections.singletonList(this.databaseComboBox.getServer().getFullAdminName()));
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.serverComboBox.addItemListener(this::onServerChanged);
        this.databaseComboBox.addItemListener(this::onDatabaseChanged);
        this.urlTextField.getDocument().addDocumentListener((TextDocumentListenerAdapter) this.debouncer::debounce);
        this.testConnectionButton.addActionListener(this::onTestConnectionButtonClicked);
        this.testConnectionActionPanel.getCopyButton().addActionListener(this::onCopyButtonClicked);
    }

    private void onTestConnectionButtonClicked(ActionEvent e) {
        if (Objects.isNull(this.jdbcUrl)) {
            return;
        }
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        final String username = usernameComboBox.getValue();
        final String password = String.valueOf(inputPasswordField.getPassword());
        final String title = String.format("Connecting to Database (%s)...", jdbcUrl.getServerHost());
        AzureTaskManager.getInstance().runInBackground(title, false, () -> {
            final DatabaseConnectionUtils.ConnectResult connectResult = DatabaseConnectionUtils.connectWithPing(this.jdbcUrl, username, password);
            // show result info
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
            testResultTextPane.setVisible(true);
            final Icon icon = connectResult.isConnected() ? AllIcons.General.InspectionsOK : AllIcons.General.BalloonError;
            testConnectionActionPanel.getIconLabel().setIcon(icon);
            testConnectionButton.setIcon(null);
            testConnectionButton.setEnabled(true);
        });
    }

    private String getConnectResultMessage(DatabaseConnectionUtils.ConnectResult result) {
        final StringBuilder messageBuilder = new StringBuilder();
        if (result.isConnected()) {
            messageBuilder.append("Connected successfully.").append(System.lineSeparator());
            messageBuilder.append("Version: ").append(result.getServerVersion()).append(System.lineSeparator());
            messageBuilder.append("Ping cost: ").append(result.getPingCost()).append("ms");
        } else {
            messageBuilder.append("Failed to connect with above parameters.").append(System.lineSeparator());
            messageBuilder.append("Message: ").append(result.getMessage());
        }
        return messageBuilder.toString();
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.serverComboBox.setSubscription(subscription);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.serverComboBox.setSubscription(null);
        }
    }

    private void onServerChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final IDatabaseServer<T> server = (IDatabaseServer<T>) e.getItem();
            this.databaseComboBox.setServer(server);
            this.usernameComboBox.setServer(server);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.databaseComboBox.setServer(null);
            this.usernameComboBox.setServer(null);
        }
    }

    private void onDatabaseChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final String server = Optional.ofNullable(this.databaseComboBox.getServer())
                .map(IDatabaseServer::getFullyQualifiedDomainName).orElse(null);
            final String database = Optional.ofNullable((IDatabase) e.getItem()).map(IDatabase::getName).orElse(null);
            this.jdbcUrl = Optional.ofNullable((IDatabase) e.getItem()).map(IDatabase::getJdbcUrl).orElse(null);
            this.urlTextField.setText(Optional.ofNullable(this.jdbcUrl).map(JdbcUrl::toString).orElse(""));
            this.urlTextField.setCaretPosition(0);
        }
    }

    private void onUrlEdited() {
        try {
            this.jdbcUrl = JdbcUrl.from(this.urlTextField.getText().trim());
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServerHost(),
                IDatabaseServer::getFullyQualifiedDomainName));
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), IDatabase::getName));
        } catch (final Exception exception) {
            this.jdbcUrl = null;
        }
        this.testConnectionButton.setEnabled(Objects.nonNull(this.jdbcUrl));
    }

    private void onCopyButtonClicked(ActionEvent e) {
        try {
            CopyPasteManager.getInstance().setContents(new StringSelection(testResultTextPane.getText()));
        } catch (final Exception exception) {
            final String error = "copy test result error";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    @Nullable
    public Resource<T> getValue() {
        final T database = databaseComboBox.getValue();
        if (Objects.isNull(database)) {
            return null;
        }
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) this.definition.define(database);
        resource.setPassword(inputPasswordField.getPassword());
        resource.setUsername(usernameComboBox.getValue());
        resource.setJdbcUrl(this.jdbcUrl);
        return resource;
    }

    public void setValue(Resource<T> data) {
        if (data instanceof InvalidResource) {
            return;
        }
        final SqlDatabaseResource<T> db = (SqlDatabaseResource<T>) data;
        final T database = data.getData();
        if (database != null) {
            final ResourceId serverId = ResourceId.fromString(database.getId()).parent();
            this.subscriptionComboBox.setValue(Azure.az(AzureAccount.class).account().getSubscription(serverId.subscriptionId()));
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(serverId.name(), AzResource::getName));
        }
        Optional.ofNullable(db.getPassword())
            .ifPresent(p -> this.inputPasswordField.setText(String.valueOf(p)));
        Optional.ofNullable(database).map(AzResource::getName).ifPresent(dbName ->
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(dbName, IDatabase::getName)));
        Optional.ofNullable(db.getUsername())
            .ifPresent((username -> this.usernameComboBox.setValue(username)));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.serverComboBox,
            this.databaseComboBox,
            this.usernameComboBox,
            this.subscriptionComboBox
        };
        return Arrays.asList(inputs);
    }

    protected void createUIComponents() {
        this.serverComboBox = this.initServerComboBox();
        this.databaseComboBox = new DatabaseComboBox<>();
        this.usernameComboBox = new UsernameComboBox();
    }

    protected abstract ServerComboBox<IDatabaseServer<T>> initServerComboBox();

    protected Database createDatabase() {
        final IDatabaseServer<T> server = this.databaseComboBox.getServer();
        final IDatabase value = this.databaseComboBox.getValue();
        return new Database(ResourceId.fromString(server.getId()), value.getName());
    }
}
