/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.mysql;

import lombok.Getter;

import javax.swing.*;

public class MySQLPropertryOverviewPanel extends JPanel {
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

    MySQLPropertryOverviewPanel() {
        super();
        disableTxtBoard();
        makeTxtOpaque();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

    private void disableTxtBoard() {
        resourceGroupTextField.setBorder(BorderFactory.createEmptyBorder());
        serverNameTextField.setBorder(BorderFactory.createEmptyBorder());
        statusTextField.setBorder(BorderFactory.createEmptyBorder());
        serverAdminLoginNameTextField.setBorder(BorderFactory.createEmptyBorder());
        locationTextField.setBorder(BorderFactory.createEmptyBorder());
        mysqlVersionTextField.setBorder(BorderFactory.createEmptyBorder());
        subscriptionTextField.setBorder(BorderFactory.createEmptyBorder());
        performanceConfigurationsTextField.setBorder(BorderFactory.createEmptyBorder());
        subscriptionIDTextField.setBorder(BorderFactory.createEmptyBorder());
        sslEnforceStatusTextField.setBorder(BorderFactory.createEmptyBorder());
    }

    private void makeTxtOpaque() {
        resourceGroupTextField.setBackground(null);
        serverNameTextField.setBackground(null);
        statusTextField.setBackground(null);
        serverAdminLoginNameTextField.setBackground(null);
        locationTextField.setBackground(null);
        mysqlVersionTextField.setBackground(null);
        subscriptionTextField.setBackground(null);
        performanceConfigurationsTextField.setBackground(null);
        subscriptionIDTextField.setBackground(null);
        sslEnforceStatusTextField.setBackground(null);
    }
}
