/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.database.AdminUsernameTextField;
import com.microsoft.azure.toolkit.intellij.database.BaseNameValidator;
import com.microsoft.azure.toolkit.intellij.database.PasswordUtils;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.util.Arrays;
import java.util.List;

public class SqlServerCreationBasicPanel extends JPanel implements AzureFormPanel<DatabaseServerConfig> {

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

    private final DatabaseServerConfig config;

    SqlServerCreationBasicPanel(DatabaseServerConfig config) {
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
        serverNameTextField.addValidator(new BaseNameValidator(serverNameTextField, (sid, name) ->
            Azure.az(AzureSqlServer.class).forSubscription(sid).checkNameAvailability(name)));
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
    public DatabaseServerConfig getValue() {
        config.setName(serverNameTextField.getText());
        config.setAdminName(adminUsernameTextField.getText());
        config.setAdminPassword(String.valueOf(passwordField.getPassword()));
        return config;
    }

    @Override
    public void setValue(DatabaseServerConfig data) {
        if (StringUtils.isNotBlank(config.getName())) {
            serverNameTextField.setText(config.getName());
        }
        if (StringUtils.isNotBlank(config.getAdminName())) {
            adminUsernameTextField.setText(config.getAdminName());
        }
        if (config.getAdminPassword() != null) {
            passwordField.setText(config.getAdminPassword());
        }
        confirmPasswordFieldInput.setValue("");
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            adminUsernameTextField,
            serverNameTextField,
            passwordFieldInput,
            confirmPasswordFieldInput
        };
        return Arrays.asList(inputs);
    }

}
