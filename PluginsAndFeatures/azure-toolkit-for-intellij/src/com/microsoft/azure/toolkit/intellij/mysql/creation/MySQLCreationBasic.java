/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.mysql.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.mysql.AdminUsernameTextField;
import com.microsoft.azure.toolkit.intellij.mysql.ServerNameTextField;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.util.Arrays;
import java.util.List;

public class MySQLCreationBasic extends JPanel implements AzureFormPanel<AzureMySQLConfig> {

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

    private AzureMySQLConfig config;

    MySQLCreationBasic(AzureMySQLConfig config) {
        super();
        this.config = config;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        init();
        initListeners();
        setData(config);
    }

    private void init() {
        passwordFieldInput = PasswordUtils.generatePasswordFieldInput(this.passwordField, this.adminUsernameTextField);
        confirmPasswordFieldInput = PasswordUtils.generateConfirmPasswordFieldInput(this.confirmPasswordField, this.passwordField);
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
    public AzureMySQLConfig getData() {
        config.setServerName(serverNameTextField.getText());
        config.setAdminUsername(adminUsernameTextField.getText());
        config.setPassword(passwordField.getPassword());
        config.setConfirmPassword(confirmPasswordField.getPassword());
        return config;
    }

    @Override
    public void setData(AzureMySQLConfig data) {
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
        final AzureFormInput<?>[] inputs = {
            serverNameTextField,
            adminUsernameTextField,
            passwordFieldInput,
            confirmPasswordFieldInput
        };
        return Arrays.asList(inputs);
    }

}
