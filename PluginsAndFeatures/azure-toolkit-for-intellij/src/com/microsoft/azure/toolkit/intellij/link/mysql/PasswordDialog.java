/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
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

public class PasswordDialog extends AzureDialog<PasswordConfig> implements AzureForm<PasswordConfig> {

    private static final String TITLE = "Credential for Azure Database for MySQL";
    private static final String HEADER_PATTERN = "Please provide credential for user (%s) to access database (%s) on server (%s).";

    private JPanel root;
    private JLabel headerIconLabel;
    private JTextPane headerTextPane;
    private JTextPane testResultTextPane;
    private JButton testConnectionButton;
    private TestConnectionActionPanel testConnectionActionPanel;
    private JPasswordField passwordField;
    private PasswordSaveComboBox passwordSaveComboBox;

    private Project project;
    private String username;
    private String url;

    public PasswordDialog(Project project, String username, String url) {
        super(project);
        this.project = project;
        this.username = username;
        this.url = url;
        setTitle(TITLE);
        JdbcUrl jdbcUrl = JdbcUrl.from(url);
        headerTextPane.setText(String.format(HEADER_PATTERN, username, jdbcUrl.getDatabase(), jdbcUrl.getHostname()));
        testConnectionButton.setEnabled(false);
        testConnectionActionPanel.setVisible(false);
        testResultTextPane.setEditable(false);
        testResultTextPane.setText(StringUtils.EMPTY);
        Dimension lastColumnSize = new Dimension(106, 30);
        passwordSaveComboBox.setPreferredSize(lastColumnSize);
        passwordSaveComboBox.setMaximumSize(lastColumnSize);
        passwordSaveComboBox.setSize(lastColumnSize);
        this.init();
        this.initListener();
    }

    private void initListener() {
        this.passwordField.addKeyListener(this.onInputPasswordFieldChanged());
        this.testConnectionButton.addActionListener(this::onTestConnectionButtonClicked);
        this.testConnectionActionPanel.getCopyButton().addActionListener(this::onCopyButtonClicked);

    }

    private KeyListener onInputPasswordFieldChanged() {
        KeyListener listener = new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                testConnectionButton.setEnabled(ArrayUtils.isNotEmpty(passwordField.getPassword()));
            }

        };
        return listener;
    }

    private void onTestConnectionButtonClicked(ActionEvent e) {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setIcon(new AnimatedIcon.Default());
        testConnectionButton.setDisabledIcon(new AnimatedIcon.Default());
        String password = String.valueOf(passwordField.getPassword());
        Runnable runnable = () -> {
            MySQLConnectionUtils.ConnectResult connectResult = MySQLConnectionUtils.connectWithPing(url, username, password);
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
        AzureTask task = new AzureTask(null, AzureBundle.message("azure.mysql.link.connection.title", jdbcUrl.getHostname()), false, runnable);
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
    public AzureForm<PasswordConfig> getForm() {
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
    public PasswordConfig getData() {
        PasswordConfig config = PasswordConfig.getDefaultConfig();
        config.setPasswordSaveType(passwordSaveComboBox.getValue());
        config.setPassword(passwordField.getPassword());
        return config;
    }

    @Override
    public void setData(PasswordConfig data) {
        passwordSaveComboBox.setValue(data.getPasswordSaveType());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {this.passwordSaveComboBox};
        return Arrays.asList(inputs);
    }

}
