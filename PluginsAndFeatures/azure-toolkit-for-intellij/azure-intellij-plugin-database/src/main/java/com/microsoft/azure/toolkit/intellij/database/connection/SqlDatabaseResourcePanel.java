/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.database.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.PasswordSaveComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.database.component.TestConnectionActionPanel;
import com.microsoft.azure.toolkit.intellij.database.component.UsernameComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azuretools.azurecommons.util.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class SqlDatabaseResourcePanel<T extends IDatabase> implements AzureFormJPanel<Resource<T>> {
    private final SqlDatabaseResourceDefinition<T> definition;
    private JPanel contentPanel;
    private SubscriptionComboBox subscriptionComboBox;
    private ServerComboBox<IDatabaseServer<T>> serverComboBox;
    private DatabaseComboBox<T> databaseComboBox;
    private UsernameComboBox usernameComboBox;
    private JPasswordField inputPasswordField;
    private JTextField urlTextField;
    private JButton testConnectionButton;
    private PasswordSaveComboBox passwordSaveComboBox;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JTextPane testResultTextPane;

    private JdbcUrl jdbcUrl;

    public SqlDatabaseResourcePanel(final SqlDatabaseResourceDefinition<T> definition, final Function<String, List<? extends IDatabaseServer<T>>> serversLoader) {
        super();
        this.definition = definition;
        init();
        initListeners();

        this.serverComboBox.setItemsLoader(() -> Objects.isNull(this.serverComboBox.getSubscription()) ? Collections.emptyList() :
            serversLoader.apply(this.serverComboBox.getSubscription().getId()));
    }

    @Override
    public JPanel getContentPanel() {
        return contentPanel;
    }

    private void init() {
        final Dimension passwordSaveComboBoxSize = new Dimension(106, passwordSaveComboBox.getPreferredSize().height);
        passwordSaveComboBox.setPreferredSize(passwordSaveComboBoxSize);
        passwordSaveComboBox.setMaximumSize(passwordSaveComboBoxSize);
        passwordSaveComboBox.setSize(passwordSaveComboBoxSize);
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testConnectionButton.setEnabled(false);
        this.passwordSaveComboBox.setValue(Arrays.stream(Password.SaveType.values())
            .filter(e -> StringUtils.equals(e.name(), Azure.az().config().getDatabasePasswordSaveType())).findAny()
            .orElse(Password.SaveType.UNTIL_RESTART));
        // username loader
        this.usernameComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ? Collections.emptyList() :
            Collections.singletonList(this.databaseComboBox.getServer().getAdminName() + "@" +
                this.databaseComboBox.getServer().getName()));
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.serverComboBox.addItemListener(this::onServerChanged);
        this.databaseComboBox.addItemListener(this::onDatabaseChanged);
        this.inputPasswordField.addKeyListener(this.onPasswordChanged());
        this.urlTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                onUrlEdited();
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
        final String title = String.format("Connecting to Database (%s)...", jdbcUrl.getServerHost());
        AzureTaskManager.getInstance().runInBackground(title, false, () -> {
            final DatabaseConnectionUtils.ConnectResult connectResult = DatabaseConnectionUtils.connectWithPing(this.jdbcUrl, username, password);
            // show result info
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
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
            this.jdbcUrl = JdbcUrl.from(this.urlTextField.getText());
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServerHost(),
                IDatabaseServer::getFullyQualifiedDomainName));
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), IDatabase::getName));
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

    @Nullable
    public Resource<T> getValue() {
        final T database = databaseComboBox.getValue();
        if (Objects.isNull(database)) {
            return null;
        }
        final Password password = new Password();
        password.password(inputPasswordField.getPassword());
        password.saveType(passwordSaveComboBox.getValue());
        final SqlDatabaseResource<T> resource = (SqlDatabaseResource<T>) this.definition.define(database);
        resource.setPassword(password);
        resource.setUsername(usernameComboBox.getValue());
        resource.setJdbcUrl(this.jdbcUrl);
        return resource;
    }

    public void setValue(Resource<T> data) {
        final SqlDatabaseResource<T> db = (SqlDatabaseResource<T>) data;
        final T database = data.getData();
        if (database != null) {
            final ResourceId serverId = ResourceId.fromString(database.getId()).parent();
            this.subscriptionComboBox.setValue(Azure.az(AzureAccount.class).account().getSubscription(serverId.subscriptionId()));
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(serverId.name(), AzResourceBase::getName));
        }
        Optional.ofNullable(db.getPassword())
            .flatMap(p -> Optional.ofNullable(p.password()))
            .ifPresent(p -> this.inputPasswordField.setText(String.valueOf(p)));
        if (Objects.nonNull(db.getPassword()) && Objects.nonNull(db.getPassword().saveType())) {
            this.passwordSaveComboBox.setValue(db.getPassword().saveType());
        } else {
            this.passwordSaveComboBox.setValue(Arrays.stream(Password.SaveType.values())
                .filter(e -> StringUtils.equals(e.name(), Azure.az().config().getDatabasePasswordSaveType())).findAny()
                .orElse(Password.SaveType.UNTIL_RESTART));
        }
        Optional.ofNullable(database).map(AzResourceBase::getName).ifPresent(dbName ->
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
        this.serverComboBox = new ServerComboBox<>();
        this.databaseComboBox = new DatabaseComboBox<>();
        this.usernameComboBox = new UsernameComboBox();
    }

    protected Database createDatabase() {
        final IDatabaseServer<T> server = this.databaseComboBox.getServer();
        final IDatabase value = this.databaseComboBox.getValue();
        return new Database(ResourceId.fromString(server.getId()), value.getName());
    }
}
