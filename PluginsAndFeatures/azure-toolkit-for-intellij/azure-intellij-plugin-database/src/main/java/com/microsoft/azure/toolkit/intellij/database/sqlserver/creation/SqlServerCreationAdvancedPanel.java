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
import com.microsoft.azure.toolkit.intellij.database.BaseNameValidator;
import com.microsoft.azure.toolkit.intellij.database.BaseRegionValidator;
import com.microsoft.azure.toolkit.intellij.database.PasswordUtils;
import com.microsoft.azure.toolkit.intellij.database.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.database.ServerNameTextField;
import com.microsoft.azure.toolkit.intellij.database.component.ConnectionSecurityPanel;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.database.DatabaseServerConfig;
import com.microsoft.azure.toolkit.lib.sqlserver.AzureSqlServer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class SqlServerCreationAdvancedPanel extends JPanel implements AzureFormPanel<DatabaseServerConfig> {

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

    private final DatabaseServerConfig config;

    SqlServerCreationAdvancedPanel(DatabaseServerConfig config) {
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
        serverNameTextField.setSubscription(config.getSubscription());
        regionComboBox.setItemsLoader(() ->
            Azure.az(AzureSqlServer.class).forSubscription(this.subscriptionComboBox.getValue().getId()).listSupportedRegions());
        regionComboBox.addValidator(new BaseRegionValidator(regionComboBox, (sid, region) ->
            Azure.az(AzureSqlServer.class).forSubscription(sid).checkRegionAvailability(region)));
        serverNameTextField.addValidator(new BaseNameValidator(serverNameTextField, (sid, name) ->
            Azure.az(AzureSqlServer.class).forSubscription(sid).checkNameAvailability(name)));
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
        config.setAzureServiceAccessAllowed(e.getStateChange() == ItemEvent.SELECTED);
    }

    private void onSecurityAllowAccessFromLocalMachineCheckBoxChanged(final ItemEvent e) {
        config.setLocalMachineAccessAllowed(e.getStateChange() == ItemEvent.SELECTED);
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
        config.setSubscription(subscriptionComboBox.getValue());
        config.setResourceGroup(resourceGroupComboBox.getValue());
        config.setRegion(regionComboBox.getValue());
        config.setAzureServiceAccessAllowed(security.getAllowAccessFromAzureServicesCheckBox().isSelected());
        config.setLocalMachineAccessAllowed(security.getAllowAccessFromLocalMachineCheckBox().isSelected());
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
            passwordField.setText(String.valueOf(config.getAdminPassword()));
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
        confirmPasswordFieldInput.setValue("");
        security.getAllowAccessFromAzureServicesCheckBox().setSelected(config.isAzureServiceAccessAllowed());
        security.getAllowAccessFromLocalMachineCheckBox().setSelected(config.isLocalMachineAccessAllowed());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.adminUsernameTextField,
            this.serverNameTextField,
            this.subscriptionComboBox,
            this.resourceGroupComboBox,
            this.regionComboBox,
            this.passwordFieldInput,
            this.confirmPasswordFieldInput
        };
        return Arrays.asList(inputs);
    }
}
