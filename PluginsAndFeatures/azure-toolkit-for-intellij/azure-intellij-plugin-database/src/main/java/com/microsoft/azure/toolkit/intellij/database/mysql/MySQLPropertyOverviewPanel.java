/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.mysql;

import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import lombok.Getter;

import javax.swing.*;

public class MySQLPropertyOverviewPanel extends JPanel {
    private JPanel rootPanel;
    @Getter
    private JTextField resourceGroupTextField;
    @Getter
    private JTextField serverNameTextField;
    @Getter
    private JTextField statusTextField;
    @Getter
    private JTextField serverAdminLoginNameTextField;
    @Getter
    private JTextField locationTextField;
    @Getter
    private JTextField mysqlVersionTextField;
    @Getter
    private JTextField subscriptionTextField;
    @Getter
    private JTextField performanceConfigurationsTextField;
    @Getter
    private JTextField subscriptionIDTextField;
    @Getter
    private JTextField sslEnforceStatusTextField;

    MySQLPropertyOverviewPanel() {
        super();
        TextFieldUtils.disableTextBoard(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, mysqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField,
            subscriptionIDTextField, sslEnforceStatusTextField);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, mysqlVersionTextField, subscriptionTextField, performanceConfigurationsTextField,
            subscriptionIDTextField, sslEnforceStatusTextField);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

}
