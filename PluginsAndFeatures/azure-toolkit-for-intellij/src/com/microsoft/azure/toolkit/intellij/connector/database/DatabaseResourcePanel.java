/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database;


import com.intellij.icons.AllIcons;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.ResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.database.component.DatabaseComboBox;
import com.microsoft.azure.toolkit.intellij.connector.database.component.PasswordSaveComboBox;
import com.microsoft.azure.toolkit.intellij.connector.database.component.ServerComboBox;
import com.microsoft.azure.toolkit.intellij.connector.database.component.TestConnectionActionPanel;
import com.microsoft.azure.toolkit.intellij.connector.database.component.UsernameComboBox;
import com.microsoft.azure.toolkit.intellij.connector.sql.SqlServerDatabaseResource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.database.JdbcUrl;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.mysql.model.MySqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.mysql.service.AzureMySql;
import com.microsoft.azure.toolkit.lib.mysql.service.MySqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.model.SqlDatabaseEntity;
import com.microsoft.azure.toolkit.lib.sqlserver.service.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.service.ISqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.service.impl.SqlServer;
import com.microsoft.azuretools.azurecommons.util.Utils;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
import java.util.stream.Collectors;

/**
 * TODO(Qianjin): refactor "if (definition instanceof SqlServerDatabaseResource.Definition)" codes once we migrate MySQL to track2 into common-msql-lib.
 */
public class DatabaseResourcePanel implements AzureFormJPanel<DatabaseResource> {
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
    private final ResourceDefinition definition;

    public DatabaseResourcePanel(ResourceDefinition definition) {
        super();
        this.definition = definition;
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
        envPrefixTextField.setText(definition instanceof SqlServerDatabaseResource.Definition ? "AZURE_SQL_" : "AZURE_MYSQL_");
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.serverComboBox.addItemListener(this::onServerChanged);
        this.databaseComboBox.addItemListener(this::onDatabaseChanged);
        this.inputPasswordField.addKeyListener(this.onPasswordChanged());
        this.urlTextField.addFocusListener(new FocusAdapter() {
            @Override
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
        final String username = (String) usernameComboBox.getValue();
        final String password = String.valueOf(inputPasswordField.getPassword());
        final String title = String.format("Connecting to Azure Database for MySQL (%s)...", jdbcUrl.getServerHost());
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
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.serverComboBox.setSubscription(subscription);
        }
    }

    private void onServerChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            this.databaseComboBox.setServer(e.getItem());
            this.usernameComboBox.setServer(e.getItem());
        }
    }

    private void onDatabaseChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            if (definition instanceof SqlServerDatabaseResource.Definition) {
                String server = Optional.ofNullable((SqlServer) this.databaseComboBox.getServer())
                        .map(sql -> sql.entity().getFullyQualifiedDomainName()).orElse(null);
                String database = Optional.ofNullable((SqlDatabaseEntity) e.getItem()).map(SqlDatabaseEntity::getName).orElse(null);
                this.jdbcUrl = Objects.isNull(this.jdbcUrl) ? JdbcUrl.sqlserver(server, database) : this.jdbcUrl.setServerHost(server).setDatabase(database);
            } else {
                String server = Optional.ofNullable((MySqlServer) this.databaseComboBox.getServer())
                        .map(mysql -> mysql.entity().getFullyQualifiedDomainName()).orElse(null);
                String database = Optional.ofNullable((MySqlDatabaseEntity) e.getItem()).map(MySqlDatabaseEntity::getName).orElse(null);
                this.jdbcUrl = Objects.isNull(this.jdbcUrl) ? JdbcUrl.mysql(server, database) : this.jdbcUrl.setServerHost(server).setDatabase(database);
            }
            this.urlTextField.setText(this.jdbcUrl.toString());
            this.urlTextField.setCaretPosition(0);
        }
    }

    private void onUrlEdited(FocusEvent e) {
        try {
            this.jdbcUrl = JdbcUrl.from(this.urlTextField.getText());
            if (definition instanceof SqlServerDatabaseResource.Definition) {
                this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServerHost(),
                        server -> ((ISqlServer) server).entity().getFullyQualifiedDomainName()));
                this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), SqlDatabaseEntity::getName));
            } else {
                this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getServerHost(),
                        server -> ((MySqlServer) server).entity().getFullyQualifiedDomainName()));
                this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(this.jdbcUrl.getDatabase(), MySqlDatabaseEntity::getName));
            }
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
    public DatabaseResource getData() {
        final Password password = new Password();
        password.password(inputPasswordField.getPassword());
        password.saveType(passwordSaveComboBox.getValue());

        final DatabaseResource resource = new DatabaseResource(definition.getType(),
                definition instanceof SqlServerDatabaseResource.Definition ?
                ((SqlDatabaseEntity) databaseComboBox.getValue()).getId() : ((MySqlDatabaseEntity) databaseComboBox.getValue()).getId());
        resource.setPassword(password);
        resource.setUsername((String) usernameComboBox.getValue());
        resource.setJdbcUrl(this.jdbcUrl);
        resource.setEnvPrefix(envPrefixTextField.getText());
        return resource;
    }

    @Override
    public void setData(DatabaseResource resource) {
        Optional.ofNullable(resource.getServerId()).ifPresent((serverId -> {
            Optional.ofNullable(serverId.subscriptionId()).ifPresent(subscriptionId -> {
                this.subscriptionComboBox.setValue(new AzureComboBox.ItemReference<>(subscriptionId, Subscription::getId), true);
            });
            Optional.ofNullable(serverId.name()).ifPresent(name -> {
                if (definition instanceof SqlServerDatabaseResource.Definition) {
                    this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(name, server -> ((ISqlServer) server).entity().getName()), true);
                } else {
                    this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(name, server -> ((MySqlServer) server).entity().getName()), true);
                }
            });
        }));
        Optional.ofNullable(resource.getPassword()).ifPresent(config -> {
            Optional.ofNullable(config.password()).ifPresent(password -> this.inputPasswordField.setText(String.valueOf(password)));
        });
        if (Objects.nonNull(resource.getPassword()) && Objects.nonNull(resource.getPassword().saveType())) {
            this.passwordSaveComboBox.setValue(resource.getPassword().saveType());
        } else {
            this.passwordSaveComboBox.setValue(Arrays.stream(Password.SaveType.values())
                .filter(e -> StringUtils.equals(e.name(), AzureConfigurations.getInstance().passwordSaveType())).findAny()
                .orElse(Password.SaveType.UNTIL_RESTART));
        }
        Optional.ofNullable(resource.getDatabaseName()).ifPresent(dbName -> {
            if (definition instanceof SqlServerDatabaseResource.Definition) {
                this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(dbName, SqlDatabaseEntity::getName), true);
            } else {
                this.databaseComboBox.setValue(new AzureComboBox.ItemReference<>(dbName, MySqlDatabaseEntity::getName), true);
            }
        });
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

    private void createUIComponents() {
        if (definition instanceof SqlServerDatabaseResource.Definition) {
            // server
            this.serverComboBox = new ServerComboBox<SqlServer>();
            this.serverComboBox.setItemsLoader(() -> Objects.isNull(this.serverComboBox.getSubscription()) ? Collections.emptyList() :
                    Azure.az(AzureSqlServer.class).subscription(this.serverComboBox.getSubscription().getId())
                            .sqlServers().stream().map(e -> (SqlServer) e).collect(Collectors.toList()));
            this.serverComboBox.setItemTextFunc((Function<SqlServer, String>) server -> server.entity().getName());
            // database
            this.databaseComboBox = new DatabaseComboBox<SqlServer, SqlDatabaseEntity>();
            this.databaseComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ?
                    Collections.emptyList() : ((SqlServer) this.databaseComboBox.getServer()).databases());
            this.databaseComboBox.setItemTextFunc((Function<SqlDatabaseEntity, String>) databaseEntity -> databaseEntity.getName());
            // username
            this.usernameComboBox = new UsernameComboBox<SqlServer>();
            this.usernameComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ? Collections.emptyList() :
                    Arrays.asList(((SqlServer) this.databaseComboBox.getServer()).entity().getAdministratorLoginName() + "@" +
                            ((SqlServer) this.databaseComboBox.getServer()).entity().getName()));
        } else {
            // server
            this.serverComboBox = new ServerComboBox<MySqlServer>();
            this.serverComboBox.setItemsLoader(() -> Objects.isNull(this.serverComboBox.getSubscription()) ? Collections.emptyList() :
                    Azure.az(AzureMySql.class).subscription(this.serverComboBox.getSubscription().getId()).list());
            this.serverComboBox.setItemTextFunc((Function<MySqlServer, String>) server -> server.entity().getName());
            // database
            this.databaseComboBox = new DatabaseComboBox<MySqlServer, MySqlDatabaseEntity>();
            this.databaseComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ?
                    Collections.emptyList() : ((MySqlServer) this.databaseComboBox.getServer()).databases());
            this.databaseComboBox.setItemTextFunc((Function<MySqlDatabaseEntity, String>) databaseEntity -> databaseEntity.getName());
            // username
            this.usernameComboBox = new UsernameComboBox<MySqlServer>();
            this.usernameComboBox.setItemsLoader(() -> Objects.isNull(this.databaseComboBox.getServer()) ? Collections.emptyList() :
                    Arrays.asList(((MySqlServer) this.databaseComboBox.getServer()).entity().getAdministratorLoginName() + "@" +
                            ((MySqlServer) this.databaseComboBox.getServer()).entity().getName()));
        }
    }
}
