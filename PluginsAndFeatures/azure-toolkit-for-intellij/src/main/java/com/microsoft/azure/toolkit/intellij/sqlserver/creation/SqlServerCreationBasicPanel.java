/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sqlserver.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.database.AdminUsernameTextField;
import com.microsoft.azure.toolkit.intellij.database.PasswordUtils;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.intellij.sqlserver.common.SqlServerNameValidator;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.util.Arrays;
import java.util.List;

public class SqlServerCreationBasicPanel extends JPanel implements AzureFormPanel<SqlServerConfig> {

    private JPanel rootPanel;
    @Getter
    private ServerNameTextField serverNameTextField;
    @Getter
    private AdminUsernameTextField adminUsernameTextField;
    @Getter
    private JPasswordField passwordField;
    @Getter
    private JPasswordField confirmPasswordField;

    private AzurePasswordFieldInput passwordFieldInput;
    private AzurePasswordFieldInput confirmPasswordFieldInput;

    private final SqlServerConfig config;

    SqlServerCreationBasicPanel(SqlServerConfig config) {
        super();
        this.config = config;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        init();
        initListeners();
        setValue(config);
    }

    private void init() {
        serverNameTextField.setSubscription(config.getSubscription());
        passwordFieldInput = PasswordUtils.generatePasswordFieldInput(this.passwordField, this.adminUsernameTextField);
        confirmPasswordFieldInput = PasswordUtils.generateConfirmPasswordFieldInput(this.confirmPasswordField, this.passwordField);
        serverNameTextField.setValidator(new SqlServerNameValidator(serverNameTextField));
    }

    private void initListeners() {
        this.adminUsernameTextField.getDocument().addDocumentListener(generateAdminUsernameListener());
    }

    private DocumentListener generateAdminUsernameListener() {
        return new TextDocumentListenerAdapter() {
            @Override
            public void onDocumentChanged() {
                if (!adminUsernameTextField.isValueInitialized()) {
                    adminUsernameTextField.setValueInitialized(true);
                }
            }
        };
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

    @Override
    public SqlServerConfig getValue() {
        config.setServerName(serverNameTextField.getText());
        config.setAdminUsername(adminUsernameTextField.getText());
        config.setPassword(passwordField.getPassword());
        config.setConfirmPassword(confirmPasswordField.getPassword());
        return config;
    }

    @Override
    public void setValue(SqlServerConfig data) {
        if (StringUtils.isNotBlank(config.getServerName())) {
            serverNameTextField.setText(config.getServerName());
        }
        if (StringUtils.isNotBlank(config.getAdminUsername())) {
            adminUsernameTextField.setText(config.getAdminUsername());
        }
        if (config.getPassword() != null) {
            passwordField.setText(String.valueOf(config.getPassword()));
        }
        if (config.getConfirmPassword() != null) {
            confirmPasswordField.setText(String.valueOf(config.getConfirmPassword()));
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(serverNameTextField, adminUsernameTextField, passwordFieldInput, confirmPasswordFieldInput);
    }

}
