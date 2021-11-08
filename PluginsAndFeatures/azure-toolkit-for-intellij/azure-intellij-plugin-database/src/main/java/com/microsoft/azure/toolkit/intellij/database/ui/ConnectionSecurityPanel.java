/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.ui;

import lombok.Getter;

import javax.swing.*;

public class ConnectionSecurityPanel extends JPanel {
    @Getter
    private JCheckBox allowAccessFromAzureServicesCheckBox;
    @Getter
    private JCheckBox allowAccessFromLocalMachineCheckBox;
    private JPanel rootPanel;

    public ConnectionSecurityPanel() {
        super();
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }
}
