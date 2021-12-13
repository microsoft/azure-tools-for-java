/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.database.postgre.connection;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.database.Database;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils;
import com.microsoft.azure.toolkit.intellij.connector.database.component.*;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabaseServer;
import com.microsoft.azure.toolkit.lib.postgre.AzurePostgreSql;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlDatabase;
import com.microsoft.azuretools.azurecommons.util.Utils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class PostgreSqlResourcePanel implements AzureFormJPanel<Resource<PostgreSqlDatabase>> {
    private JPanel contentPanel;
    private SubscriptionComboBox subscriptionComboBox;
    private ServerComboBox<IDatabaseServer> serverComboBox;
    private DatabaseComboBoxV2 databaseComboBox;
    private UsernameComboBox usernameComboBox;
    private JPasswordField inputPasswordField;
    private JTextField urlTextField;
    private JButton testConnectionButton;
    private PasswordSaveComboBox passwordSaveComboBox;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JTextPane testResultTextPane;

    private JdbcUrl jdbcUrl;

    public PostgreSqlResourcePanel() {
        super();
        init();
        initListeners();

        this.jdbcUrl = JdbcUrl.postgre(StringUtils.EMPTY, StringUtils.EMPTY);
        this.serverComboBox.setItemsLoader(() -> Objects.isNull(this.serverComboBox.getSubscription()) ? Collections.emptyList() :
                Azure.az(AzurePostgreSql.class).subscription(this.serverComboBox.getSubscription().getId()).list());
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
        // database loader
        this.databaseComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ||
                !StringUtils.equalsIgnoreCase("Ready", this.databaseComboBox.getServer().entity().getState()) ?
                Collections.emptyList() : this.databaseComboBox.getServer().databasesV2());
        // username loader
        this.usernameComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ? Collections.emptyList() :
                Collections.singletonList(this.databaseComboBox.getServer().entity().getAdministratorLoginName() + "@" +
                        this.databaseComboBox.getServer().entity().getName()));
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
        final String title = String.format("Connecting to Azure Database for PostgreSQL (%s)...", jdbcUrl.getServerHost());
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
            final IDatabaseServer server = (IDatabaseServer) e.getItem();
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
                    .map(s -> s.entity().getFullyQualifiedDomainName()).orElse(null);
            final String database = Optional.ofNullable((IDatabase) e.getItem()).map(IDatabase::getName).orElse(null);
            this.jdbcUrl.setServerHost(server).setDatabase(database);
            this.urlTextField.setText(this.jdbcUrl.toString());
            this.urlTextField.setCaretPosition(0);
        }
    }

    private void onUrlEdited() {
        try {
            this.jdbcUrl = JdbcUrl.from(this.urlTextField.getText());
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServerHost(),
                    server -> server.entity().getFullyQualifiedDomainName()));
            this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), IDatabase::getName));
        } catch (final Exception exception) {
            // TODO: messager.warning(...)
        }
    }

    private void onCopyButtonClicked(ActionEvent e) {
        Utils.copyToSystemClipboard(testResultTextPane.getText());
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

    public Resource<PostgreSqlDatabase> getValue() {
        PostgreSqlDatabase database = (PostgreSqlDatabase) databaseComboBox.getValue();
        final Password password = new Password();
        password.password(inputPasswordField.getPassword());
        password.saveType(passwordSaveComboBox.getValue());
        PostgreSqlDatabaseResource resource = (PostgreSqlDatabaseResource) PostgreSqlResourceDefinition.INSTANCE.define(database);
        resource.setPassword(password);
        resource.setUsername(usernameComboBox.getValue());
        resource.setJdbcUrl(this.jdbcUrl);
        return resource;
    }

    public void setValue(Resource<PostgreSqlDatabase> data) {
        PostgreSqlDatabaseResource db = (PostgreSqlDatabaseResource) data;
        PostgreSqlDatabase database = db.getData();
        if (database != null) {
            ResourceId serverId = ResourceId.fromString(database.entity().getId()).parent();
            this.subscriptionComboBox.setValue(Azure.az(AzureAccount.class).account().getSubscription(serverId.subscriptionId()));
            this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(serverId.name(), server -> server.entity().getName()));
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
        Optional.ofNullable(database.getName()).ifPresent(dbName ->
                this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(dbName, IDatabase::getName)));
        Optional.ofNullable(db.getUsername())
                .ifPresent((username -> this.usernameComboBox.setValue(username)));
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

    protected void createUIComponents() {
        this.serverComboBox = new ServerComboBox<>();
        this.databaseComboBox = new DatabaseComboBoxV2();
        this.usernameComboBox = new UsernameComboBox();
    }

    protected Database createDatabase() {
        final IDatabaseServer server = this.databaseComboBox.getServer();
        final IDatabase value = this.databaseComboBox.getValue();
        return new Database(ResourceId.fromString(server.entity().getId()), value.getName());
    }
}
