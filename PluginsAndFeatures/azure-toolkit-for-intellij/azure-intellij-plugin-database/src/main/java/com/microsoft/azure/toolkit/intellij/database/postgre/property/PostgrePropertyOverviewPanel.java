/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.postgre.property;

import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.postgre.model.PostgreSqlServerEntity;

import javax.swing.*;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class PostgrePropertyOverviewPanel extends JPanel {
    private JPanel rootPanel;

    private JTextField resourceGroupTextField;

    private JTextField serverNameTextField;

    private JTextField statusTextField;

    private JTextField serverAdminLoginNameTextField;

    private JTextField locationTextField;

    private JTextField postgreSqlVersionTextField;

    private JTextField subscriptionTextField;

    private JTextField performanceConfigurationsTextField;

    private JTextField subscriptionIDTextField;

    private JTextField sslEnforceStatusTextField;

    PostgrePropertyOverviewPanel() {
        super();
        TextFieldUtils.disableTextBoard(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, postgreSqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField,
            subscriptionIDTextField, sslEnforceStatusTextField);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, postgreSqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField,
            subscriptionIDTextField, sslEnforceStatusTextField);
    }

    public void setFormData(PostgreSqlServer server) {
        final PostgreSqlServerEntity entity = server.entity();
        final Subscription subscription = az(AzureAccount.class).account().getSubscription(entity.getSubscriptionId());
        if (subscription != null) {
            subscriptionTextField.setText(subscription.getName());
        }
        resourceGroupTextField.setText(server.entity().getResourceGroupName());
        statusTextField.setText(server.entity().getState());
        locationTextField.setText(server.entity().getRegion().getLabel());
        subscriptionIDTextField.setText(entity.getSubscriptionId());
        serverNameTextField.setText(server.entity().getFullyQualifiedDomainName());
        serverNameTextField.setCaretPosition(0);
        serverAdminLoginNameTextField.setText(server.entity().getAdministratorLoginName() + "@" + server.name());
        serverAdminLoginNameTextField.setCaretPosition(0);
        postgreSqlVersionTextField.setText(server.entity().getVersion());
        final String skuTier = server.entity().getSkuTier();
        final int skuCapacity = server.entity().getVCore();
        final int storageGB = server.entity().getStorageInMB() / 1024;
        final String performanceConfigurations = skuTier + ", " + skuCapacity + " vCore(s), " + storageGB + " GB";
        performanceConfigurationsTextField.setText(performanceConfigurations);
        sslEnforceStatusTextField.setText(server.entity().getSslEnforceStatus());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

}
