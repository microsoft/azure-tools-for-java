/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.sqlserver.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.database.AdminUsernameTextField;
import com.microsoft.azure.toolkit.intellij.database.PasswordUtils;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.common.SqlServerRegionValidator;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.intellij.database.sqlserver.common.SqlServerNameValidator;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import com.microsoft.azure.toolkit.lib.sqlserver.SqlServerConfig;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class SqlServerCreationAdvancedPanel extends JPanel implements AzureFormPanel<SqlServerConfig> {

    private JPanel rootPanel;
    private ConnectionSecurityPanel security;
    @Getter
    private SubscriptionComboBox subscriptionComboBox;
    @Getter
    private ResourceGroupComboBox resourceGroupComboBox;
    @Getter
    private ServerNameTextField serverNameTextField;
    @Getter
    private RegionComboBox regionComboBox;
    @Getter
    private AdminUsernameTextField adminUsernameTextField;
    @Getter
    private JPasswordField passwordField;
    @Getter
    private JPasswordField confirmPasswordField;

    private AzurePasswordFieldInput passwordFieldInput;
    private AzurePasswordFieldInput confirmPasswordFieldInput;

    private final SqlServerConfig config;

    SqlServerCreationAdvancedPanel(SqlServerConfig config) {
        super();
        this.config = config;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        init();
        initListeners();
        setValue(config);
    }

    private void init() {
        passwordFieldInput = PasswordUtils.generatePasswordFieldInput(this.passwordField, this.adminUsernameTextField);
        confirmPasswordFieldInput = PasswordUtils.generateConfirmPasswordFieldInput(this.confirmPasswordField, this.passwordField);
        regionComboBox.setItemsLoader(() -> Azure.az(AzureSqlServer.class).listSupportedRegions(this.subscriptionComboBox.getValue().getId()));
        regionComboBox.setValidator(new SqlServerRegionValidator(regionComboBox));
        serverNameTextField.setSubscription(config.getSubscription());
        serverNameTextField.setValidator(new SqlServerNameValidator(serverNameTextField));
    }

    private void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.adminUsernameTextField.getDocument().addDocumentListener(this.onAdminUsernameChanged());
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.resourceGroupComboBox.setSubscription(subscription);
            this.serverNameTextField.setSubscription(subscription);
            this.regionComboBox.setSubscription(subscription);
        }
    }

    private DocumentListener onAdminUsernameChanged() {
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
        config.setSubscription(subscriptionComboBox.getValue());
        config.setResourceGroup(resourceGroupComboBox.getValue());
        config.setRegion(regionComboBox.getValue());
        config.setAllowAccessFromAzureServices(security.getAllowAccessFromAzureServicesCheckBox().isSelected());
        config.setAllowAccessFromLocalMachine(security.getAllowAccessFromLocalMachineCheckBox().isSelected());
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
        if (config.getSubscription() != null) {
            subscriptionComboBox.setValue(config.getSubscription());
        }
        if (config.getResourceGroup() != null) {
            resourceGroupComboBox.setValue(config.getResourceGroup());
        }
        if (config.getRegion() != null) {
            regionComboBox.setValue(config.getRegion());
        }
        security.getAllowAccessFromAzureServicesCheckBox().setSelected(config.isAllowAccessFromAzureServices());
        security.getAllowAccessFromLocalMachineCheckBox().setSelected(config.isAllowAccessFromLocalMachine());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.serverNameTextField,
            this.adminUsernameTextField,
            this.subscriptionComboBox,
            this.resourceGroupComboBox,
            this.regionComboBox,
            this.passwordFieldInput,
            this.confirmPasswordFieldInput
        };
        return Arrays.asList(inputs);
    }

}
