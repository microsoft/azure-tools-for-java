/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.mysql;


import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.PasswordSaveComboBox;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.TestConnectionActionPanel;
import com.microsoft.azure.toolkit.intellij.connector.mysql.component.UsernameComboBox;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.util.Utils;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MySQLDatabaseResourcePanel implements AzureFormJPanel<MySQLDatabaseResource> {
    @Getter
    private JPanel contentPanel;
    private SubscriptionComboBox subscriptionComboBox;
    private ServerComboBox serverComboBox;
    private DatabaseComboBox databaseComboBox;
    private UsernameComboBox usernameComboBox;
    private JPasswordField inputPasswordField;
    private JTextField envPrefixTextField;
    private PasswordSaveComboBox passwordSaveComboBox;
    private JTextField urlTextField;
    private JButton testConnectionButton;
    private JTextPane testResultTextPane;
    private TestConnectionActionPanel testConnectionActionPanel;

    private JdbcUrl jdbcUrl = null;

    public MySQLDatabaseResourcePanel() {
        super();
        init();
        initListeners();
    }

    private void init() {
        final Dimension lastColumnSize = new Dimension(106, 30);
        passwordSaveComboBox.setPreferredSize(lastColumnSize);
        passwordSaveComboBox.setMaximumSize(lastColumnSize);
        passwordSaveComboBox.setSize(lastColumnSize);
        envPrefixTextField.setPreferredSize(lastColumnSize);
        envPrefixTextField.setMaximumSize(lastColumnSize);
        envPrefixTextField.setSize(lastColumnSize);
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testConnectionButton.setEnabled(false);
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.serverComboBox.addItemListener(this::onServerChanged);
        this.databaseComboBox.addItemListener(this::onDatabaseChanged);
        this.inputPasswordField.addKeyListener(this.onPasswordChanged());
        this.urlTextField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                onUrlEdited(e);
            }
        });
        this.testConnectionButton.addActionListener(this::onTestConnectionButtonClicked);
        this.testConnectionActionPanel.getCopyButton().addActionListener(this::onCopyButtonClicked);
    }

    private void onTestConnectionButtonClicked(ActionEvent e) {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        final String username = usernameComboBox.getValue();
        final String password = String.valueOf(inputPasswordField.getPassword());
        final String title = String.format("Connecting to Azure Database for MySQL (%s)...", jdbcUrl.getHost());
        AzureTaskManager.getInstance().runInBackground(title, false, () -> {
            final MySQLConnectionUtils.ConnectResult connectResult = MySQLConnectionUtils.connectWithPing(this.jdbcUrl, username, password);
            // show result info
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
            final Icon icon = connectResult.isConnected() ? AllIcons.General.InspectionsOK : AllIcons.General.BalloonError;
            testConnectionActionPanel.getIconLabel().setIcon(icon);
            testConnectionButton.setIcon(null);
            testConnectionButton.setEnabled(true);
        });
    }

    private String getConnectResultMessage(MySQLConnectionUtils.ConnectResult result) {
        final StringBuilder messageBuilder = new StringBuilder();
        if (result.isConnected()) {
            messageBuilder.append("Connected successfully.").append(System.lineSeparator());
            messageBuilder.append("MySQL version: ").append(result.getServerVersion()).append(System.lineSeparator());
            messageBuilder.append("Ping cost: ").append(result.getPingCost()).append("ms");
        } else {
            messageBuilder.append("Failed to connect with above parameters.").append(System.lineSeparator());
            messageBuilder.append("Message: ").append(result.getMessage());
        }
        return messageBuilder.toString();
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.serverComboBox.setSubscription(subscription);
        }
    }

    private void onServerChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Server server = (Server) e.getItem();
            this.databaseComboBox.setServer(server);
            this.usernameComboBox.setServer(server);
        }
    }

    private void onDatabaseChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final String server = Optional.ofNullable(this.serverComboBox.getValue()).map(Server::fullyQualifiedDomainName).orElse(null);
            final String database = Optional.ofNullable((DatabaseInner) e.getItem()).map(DatabaseInner::name).orElse(null);
            this.jdbcUrl = this.jdbcUrl == null ? JdbcUrl.mysql(server, database) : this.jdbcUrl.setServer(server).setDatabase(database);
            this.urlTextField.setText(this.jdbcUrl.toString());
            this.urlTextField.setCaretPosition(0);
        }
    }

    private void onUrlEdited(FocusEvent e) {
        try {
            this.jdbcUrl = new JdbcUrl(this.urlTextField.getText());
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServer(), Server::fullyQualifiedDomainName));
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), DatabaseInner::name));
        } catch (final Exception exception) {
            // TODO: messager.warning(...)
        }
    }

    private void onCopyButtonClicked(ActionEvent e) {
        try {
            Utils.copyToSystemClipboard(testResultTextPane.getText());
        } catch (final Exception exception) {
            final String error = "copy test result error";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    private KeyListener onPasswordChanged() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (ArrayUtils.isNotEmpty(inputPasswordField.getPassword())) {
                    testConnectionButton.setEnabled(true);
                }
            }
        };
    }

    @Override
    public MySQLDatabaseResource getData() {
        final Password password = new Password();
        password.password(inputPasswordField.getPassword());
        password.saveType(passwordSaveComboBox.getValue());

        final MySQLDatabaseResource resource = new MySQLDatabaseResource(databaseComboBox.getValue().id());
        resource.setPassword(password);
        resource.setUsername(usernameComboBox.getValue());
        resource.setJdbcUrl(this.jdbcUrl);
        resource.setEnvPrefix(envPrefixTextField.getText());
        return resource;
    }

    @Override
    public void setData(MySQLDatabaseResource resource) {
        Optional.ofNullable(resource.getServerId()).ifPresent((serverId -> {
            this.subscriptionComboBox.setValue(new AzureComboBox.ItemReference<>(serverId.subscriptionId(), Subscription::getId), true);
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(serverId.name(), Server::name), true);
        }));
        Optional.ofNullable(resource.getPassword()).ifPresent(config -> {
            this.inputPasswordField.setText(String.valueOf(config.password()));
            this.passwordSaveComboBox.setValue(config.saveType());
        });
        Optional.ofNullable(resource.getDatabaseName()).ifPresent((dbName -> {
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(dbName, DatabaseInner::name), true);
        }));
        Optional.ofNullable(resource.getUsername())
            .ifPresent((username -> this.usernameComboBox.setValue(username)));
        Optional.ofNullable(resource.getEnvPrefix())
            .ifPresent(p -> this.envPrefixTextField.setText(p));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.subscriptionComboBox,
            this.serverComboBox,
            this.databaseComboBox,
            this.usernameComboBox
        };
        return Arrays.asList(inputs);
    }
}
