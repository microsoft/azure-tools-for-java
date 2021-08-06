/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.database.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.settings.AzureConfigurations;
import com.microsoft.azure.toolkit.intellij.connector.Password;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseConnectionUtils;
import com.microsoft.azure.toolkit.intellij.connector.database.DatabaseResource;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.ui.messages.AzureBundle;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PasswordDialog extends AzureDialog<Password> implements AzureForm<Password> {

    private static final String TITLE = "Credential for %s";
    private static final String HEADER_PATTERN = "Please provide credential for user (%s) to access database (%s) on server (%s).";

    private JPanel root;
    private JLabel headerIconLabel;
    private JTextPane headerTextPane;
    private JTextPane testResultTextPane;
    private JButton testConnectionButton;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JPasswordField passwordField;
    private PasswordSaveComboBox passwordSaveComboBox;

    private final DatabaseResource resource;

    public PasswordDialog(Project project, DatabaseResource resource) {
        super(project);
        this.resource = resource;
        setTitle(String.format(TITLE, resource.getTitle()));
        headerTextPane.setText(String.format(HEADER_PATTERN, resource.getUsername(), resource.getJdbcUrl().getDatabase(),
                resource.getJdbcUrl().getServerHost()));
        testConnectionButton.setEnabled(false);
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testResultTextPane.setText(StringUtils.EMPTY);
        final Dimension lastColumnSize = new Dimension(106, this.passwordSaveComboBox.getPreferredSize().height);
        this.passwordSaveComboBox.setPreferredSize(lastColumnSize);
        this.passwordSaveComboBox.setMaximumSize(lastColumnSize);
        this.passwordSaveComboBox.setSize(lastColumnSize);
        this.setData(resource.getPassword());
        this.init();
        this.initListener();
    }

    private void initListener() {
        this.passwordField.addKeyListener(this.onInputPasswordFieldChanged());
        this.testConnectionButton.addActionListener(this::onTestConnectionButtonClicked);
        this.testConnectionActionPanel.getCopyButton().addActionListener(this::onCopyButtonClicked);

    }

    private KeyListener onInputPasswordFieldChanged() {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                testConnectionButton.setEnabled(ArrayUtils.isNotEmpty(passwordField.getPassword()));
            }
        };
    }

    private void onTestConnectionButtonClicked(ActionEvent e) {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        final String password = String.valueOf(passwordField.getPassword());
        final Runnable runnable = () -> {
            final DatabaseConnectionUtils.ConnectResult connectResult = DatabaseConnectionUtils
                    .connectWithPing(this.resource.getJdbcUrl(), this.resource.getUsername(), password);
            testConnectionActionPanel.setVisible(true);
            testResultTextPane.setText(getConnectResultMessage(connectResult));
            final Icon icon = connectResult.isConnected() ? AllIcons.General.InspectionsOK : AllIcons.General.BalloonError;
            testConnectionActionPanel.getIconLabel().setIcon(icon);
            testConnectionButton.setIcon(null);
            testConnectionButton.setEnabled(true);
        };
        final String title = AzureBundle.message("azure.mysql.link.connection.title", this.resource.getJdbcUrl().getServerHost());
        final AzureTask<Void> task = new AzureTask<>(null, title, false, runnable);
        AzureTaskManager.getInstance().runInBackground(task);
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

    private void onCopyButtonClicked(ActionEvent e) {
        try {
            Utils.copyToSystemClipboard(testResultTextPane.getText());
        } catch (final Exception exception) {
            final String error = "copy test result error";
            final String action = "try again later.";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }

    @Override
    public AzureForm<Password> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return TITLE;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return root;
    }

    @Override
    public Password getData() {
        final Password config = new Password();
        config.saveType(passwordSaveComboBox.getValue());
        config.password(passwordField.getPassword());
        return config;
    }

    @Override
    public void setData(Password data) {
        this.passwordSaveComboBox.setValue(Optional.ofNullable(data).map(e -> e.saveType()).orElse(Arrays.stream(Password.SaveType.values())
                .filter(e -> StringUtils.equals(e.name(), AzureConfigurations.getInstance().passwordSaveType())).findAny()
                .orElse(Password.SaveType.UNTIL_RESTART)));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {this.passwordSaveComboBox};
        return Arrays.asList(inputs);
    }

}
