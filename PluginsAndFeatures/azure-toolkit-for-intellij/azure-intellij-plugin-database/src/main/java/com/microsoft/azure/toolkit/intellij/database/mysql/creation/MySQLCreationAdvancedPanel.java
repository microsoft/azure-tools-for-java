/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql.creation;

import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.database.AdminUsernameTextField;
import com.microsoft.azure.toolkit.intellij.database.PasswordUtils;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.intellij.database.mysql.MySQLNameValidator;
import com.microsoft.azure.toolkit.intellij.database.mysql.MySQLRegionValidator;
import com.microsoft.azure.toolkit.intellij.database.mysql.VersionComboBox;
import com.microsoft.azure.toolkit.intellij.database.ui.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySQLConfig;
import com.microsoft.azure.toolkit.lib.mysql.AzureMySql;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class MySQLCreationAdvancedPanel extends JPanel implements AzureFormPanel<AzureMySQLConfig> {

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
    private VersionComboBox versionComboBox;
    @Getter
    private AdminUsernameTextField adminUsernameTextField;
    @Getter
    private JPasswordField passwordField;
    @Getter
    private JPasswordField confirmPasswordField;

    private AzurePasswordFieldInput passwordFieldInput;
    private AzurePasswordFieldInput confirmPasswordFieldInput;

    private final AzureMySQLConfig config;

    MySQLCreationAdvancedPanel(AzureMySQLConfig config) {
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
        regionComboBox.setValidator(new MySQLRegionValidator(regionComboBox));
        regionComboBox.setItemsLoader(() -> Azure.az(AzureMySql.class).listSupportedRegions(this.subscriptionComboBox.getValue().getId()));
        serverNameTextField.setSubscription(config.getSubscription());
        serverNameTextField.setValidator(new MySQLNameValidator(serverNameTextField));
    }

    private void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.adminUsernameTextField.getDocument().addDocumentListener(generateAdminUsernameListener());
        this.security.getAllowAccessFromAzureServicesCheckBox().addItemListener(this::onSecurityAllowAccessFromAzureServicesCheckBoxChanged);
        this.security.getAllowAccessFromLocalMachineCheckBox().addItemListener(this::onSecurityAllowAccessFromLocalMachineCheckBoxChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.resourceGroupComboBox.setSubscription(subscription);
            this.serverNameTextField.setSubscription(subscription);
            this.regionComboBox.setSubscription(subscription);
        }
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

    private void onSecurityAllowAccessFromAzureServicesCheckBoxChanged(final ItemEvent e) {
        config.setAllowAccessFromAzureServices(e.getStateChange() == ItemEvent.SELECTED);
    }

    private void onSecurityAllowAccessFromLocalMachineCheckBoxChanged(final ItemEvent e) {
        config.setAllowAccessFromLocalMachine(e.getStateChange() == ItemEvent.SELECTED);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

    @Override
    public AzureMySQLConfig getValue() {
        config.setServerName(serverNameTextField.getText());
        config.setAdminUsername(adminUsernameTextField.getText());
        config.setPassword(passwordField.getPassword());
        config.setConfirmPassword(confirmPasswordField.getPassword());
        config.setSubscription(subscriptionComboBox.getValue());
        config.setResourceGroup(resourceGroupComboBox.getValue());
        config.setRegion(regionComboBox.getValue());
        if (StringUtils.isNotBlank(versionComboBox.getValue())) {
            config.setVersion(versionComboBox.getValue());
        }
        config.setAllowAccessFromAzureServices(security.getAllowAccessFromAzureServicesCheckBox().isSelected());
        config.setAllowAccessFromLocalMachine(security.getAllowAccessFromLocalMachineCheckBox().isSelected());
        return config;
    }

    @Override
    public void setValue(AzureMySQLConfig data) {
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
        if (config.getVersion() != null) {
            versionComboBox.setValue(config.getVersion());
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
            this.versionComboBox,
            this.passwordFieldInput,
            this.confirmPasswordFieldInput
        };
        return Arrays.asList(inputs);
    }

}
