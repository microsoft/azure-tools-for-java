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

package com.microsoft.intellij.helpers.mysql;

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
