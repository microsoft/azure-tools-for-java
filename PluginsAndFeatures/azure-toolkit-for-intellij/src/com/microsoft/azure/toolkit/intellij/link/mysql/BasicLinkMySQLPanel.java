/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.DatabaseInner;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ModuleComboBox;
import com.microsoft.azure.toolkit.intellij.link.LinkConfig;
import com.microsoft.azure.toolkit.intellij.link.ModuleResourceConfig;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
@Setter
public class BasicLinkMySQLPanel<T extends LinkConfig<MySQLResourceConfig, ModuleResourceConfig>> extends JPanel implements AzureFormPanel<T> {
    private SubscriptionComboBox subscriptionComboBox;
    private ServerComboBox serverComboBox;
    private DatabaseComboBox databaseComboBox;
    private UsernameComboBox usernameComboBox;
    private JPasswordField inputPasswordField;
    private JPanel rootPanel;
    private JTextField envPrefixTextField;
    private PasswordSaveComboBox passwordSaveComboBox;
    private JTextField urlTextField;
    private JButton testConnectionButton;
    private ModuleComboBox moduleComboBox;
    private JTextPane testResultTextPane;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JTextField portTextField;

    private final Supplier<T> supplier;
    private AzureWizardStep wizardStep;
    private Project project;

    private String urlHead = "jdbc:mysql://";
    private String urlMiddle = ":3306/";
    private String urlTail = "?serverTimezone=UTC&useSSL=true&requireSSL=false";

    public BasicLinkMySQLPanel(Project project, final Supplier<T> supplier) {
        super();
        this.project = project;
        this.supplier = supplier;
        init();
        initListeners();
    }

    private void init() {
        Dimension lastColumnSize = new Dimension(106, 30);
        passwordSaveComboBox.setPreferredSize(lastColumnSize);
        passwordSaveComboBox.setMaximumSize(lastColumnSize);
        passwordSaveComboBox.setSize(lastColumnSize);
        envPrefixTextField.setPreferredSize(lastColumnSize);
        envPrefixTextField.setMaximumSize(lastColumnSize);
        envPrefixTextField.setSize(lastColumnSize);
        T config = supplier.get();
        Optional.ofNullable(config.getResource().getSubscription())
                .ifPresent((subscription -> {
                    this.subscriptionComboBox.setValue(new AzureComboBox.ItemReference<>(subscription.subscriptionId(), Subscription::subscriptionId));
                    subscriptionComboBox.setForceDisable(true);
                    subscriptionComboBox.setEditable(false);
                }));
        Optional.ofNullable(config.getResource().getServer())
                .ifPresent((server -> {
                    this.serverComboBox.setValue(new AzureComboBox.ItemReference<>(server.fullyQualifiedDomainName(), Server::fullyQualifiedDomainName));
                    serverComboBox.setForceDisable(true);
                    serverComboBox.setEditable(false);
                }));
        Optional.ofNullable(config.getModule().getModule())
                .ifPresent((module -> {
                    this.moduleComboBox.setValue(new AzureComboBox.ItemReference<>(module.getName(), Module::getName));
                    moduleComboBox.setForceDisable(true);
                    moduleComboBox.setEditable(false);
                }));
        if (Objects.nonNull(config.getResource().getPasswordConfig())
                && Objects.nonNull(config.getResource().getPasswordConfig().getPasswordSaveType())) {
            passwordSaveComboBox.setValue(config.getResource().getPasswordConfig().getPasswordSaveType());
        }
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testConnectionButton.setEnabled(false);
    }

    protected void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.serverComboBox.addItemListener(this::onServerChanged);
        this.databaseComboBox.addItemListener(this::onDatabaseChanged);
        this.inputPasswordField.addKeyListener(this.onInputPasswordFieldChanged());
        this.urlTextField.addFocusListener(this.onUrlTextFieldChanged());
        this.testConnectionButton.addActionListener(this::onTestConnectionButtonClicked);
        this.testConnectionActionPanel.getCopyButton().addActionListener(this::onCopyButtonClicked);
    }

    private void onTestConnectionButtonClicked(ActionEvent e) {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        String url = urlTextField.getText();
        String username = usernameComboBox.getValue();
        String password = String.valueOf(inputPasswordField.getPassword());
        Runnable runnable = () -> {
            MySQLConnectionUtils.ConnectResult connectResult = MySQLConnectionUtils.connectWithPing(url, username, password);
            // show result info
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
            if (connectResult.isConnected()) {
                testConnectionActionPanel.getIconLabel().setIcon(AllIcons.General.InspectionsOK);
            } else {
                testConnectionActionPanel.getIconLabel().setIcon(AllIcons.General.BalloonError);
            }
            testConnectionButton.setIcon(null);
            testConnectionButton.setEnabled(true);
        };
        JdbcUrl jdbcUrl = JdbcUrl.from(url);
        AzureTask task = new AzureTask(null, String.format("Connecting to Azure Database for MySQL (%s)...", jdbcUrl.getHostname()), false, runnable);
        AzureTaskManager.getInstance().runInBackground(task);
    }

    private String getConnectResultMessage(MySQLConnectionUtils.ConnectResult result) {
        StringBuilder messageBuilder = new StringBuilder();
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
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.serverComboBox.setSubscription(subscription);
            this.databaseComboBox.setSubscription(subscription);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.serverComboBox.setSubscription(null);
            this.databaseComboBox.setSubscription(null);

        }
    }

    private void onServerChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getItem() instanceof Server) {
                final Server server = (Server) e.getItem();
                this.databaseComboBox.setServer(server);
                this.usernameComboBox.setServer(server);
                this.urlTextField.setText(buildUrl(serverComboBox.getValue(), databaseComboBox.getValue()));
                this.urlTextField.setCaretPosition(0);
            }
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.databaseComboBox.setServer(null);
            this.usernameComboBox.setServer(null);
        }
    }

    private void onDatabaseChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getItem() instanceof DatabaseInner) {
                this.urlTextField.setText(buildUrl(serverComboBox.getValue(), databaseComboBox.getValue()));
                this.urlTextField.setCaretPosition(0);
            }
        }
    }

    private KeyListener onInputPasswordFieldChanged() {
        KeyListener listener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (ArrayUtils.isNotEmpty(inputPasswordField.getPassword())) {
                    testConnectionButton.setEnabled(true);
                }
            }
        };
        return listener;
    }

    private FocusListener onUrlTextFieldChanged() {
        FocusListener listener = new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e) {
                String url = urlTextField.getText();
                String hostname = serverComboBox.getValue().fullyQualifiedDomainName();
                urlHead = url.substring(0, url.indexOf(hostname));
                String databaseName = databaseComboBox.getValue().name();
                urlTail = url.substring(url.indexOf(databaseName) + databaseName.length());
            }
        };
        return listener;
    }

    private void onCopyButtonClicked(ActionEvent e) {
        try {
            Utils.copyToSystemClipboard(testResultTextPane.getText());
        } catch (Exception exception) {
            String error = "copy test result error";
            String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    @Override
    public T getData() {
        final T config = supplier.get();
        MySQLResourceConfig source = config.getResource();
        source.setSubscription(subscriptionComboBox.getValue());
        source.setServer(serverComboBox.getValue());
        source.setDatabase(databaseComboBox.getValue());
        source.setUsername(usernameComboBox.getValue());
        PasswordConfig passwordConfig = Objects.nonNull(source.getPasswordConfig()) ? source.getPasswordConfig() : new PasswordConfig();
        passwordConfig.setPassword(inputPasswordField.getPassword());
        passwordConfig.setPasswordSaveType(passwordSaveComboBox.getValue());
        source.setPasswordConfig(passwordConfig);
        source.setUrl(urlTextField.getText());
        ModuleResourceConfig target = config.getModule();
        target.setModule(moduleComboBox.getValue());
        config.setEnvPrefix(envPrefixTextField.getText());
        return config;
    }

    @Override
    public void setData(T data) {
        MySQLResourceConfig source = data.getResource();
        this.subscriptionComboBox.setValue(source.getSubscription());
        this.serverComboBox.setValue(source.getServer());
        this.databaseComboBox.setValue(source.getDatabase());
        this.usernameComboBox.setValue(source.getUsername());
        if (Objects.nonNull(source.getPasswordConfig())) {
            this.inputPasswordField.setText(String.valueOf(source.getPasswordConfig().getPassword()));
            this.passwordSaveComboBox.setValue(source.getPasswordConfig().getPasswordSaveType());
        }
        ModuleResourceConfig target = data.getModule();
        this.moduleComboBox.setValue(target.getModule());
        this.envPrefixTextField.setText(data.getEnvPrefix());
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

    private String buildUrl(Server server, DatabaseInner database) {
        StringBuilder builder = new StringBuilder();
        if (Objects.nonNull(server) && StringUtils.isNotBlank(server.fullyQualifiedDomainName())) {
            builder.append(urlHead).append(server.fullyQualifiedDomainName());
        }
        if (Objects.nonNull(database) && StringUtils.isNotBlank(database.name())) {
            builder.append(urlMiddle).append(database.name());
        }
        builder.append(urlTail);
        return builder.toString();
    }

    private void createUIComponents() {
        moduleComboBox = new ModuleComboBox(project);
    }
}
