/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.sqlserver.properties;

import com.microsoft.azure.toolkit.intellij.common.ui.TextFieldUtils;
import lombok.Getter;

import javax.swing.*;

public class SqlServerPropertryOverviewPanel extends JPanel {
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
    private JTextField versionTextField;
    @Getter
    private JTextField subscriptionTextField;
    @Getter
    private JTextField subscriptionIDTextField;

    SqlServerPropertryOverviewPanel() {
        super();
        TextFieldUtils.disableTextBoard(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, versionTextField, subscriptionTextField, subscriptionIDTextField);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, serverNameTextField, statusTextField, serverAdminLoginNameTextField,
            locationTextField, versionTextField, subscriptionTextField, subscriptionIDTextField);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

}
