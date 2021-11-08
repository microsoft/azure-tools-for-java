/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.ui;

import lombok.Getter;

import javax.swing.*;

public class ConnectionStringsOutputPanel extends JPanel {
    @Getter
    private JTextArea outputTextArea;
    private JPanel rootPanel;
    @Getter
    private JButton copyButton;
    @Getter
    private JLabel titleLabel;
    @Getter
    private JTextPane outputTextPane;

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        rootPanel.setVisible(visible);
    }

    private void createUIComponents() {
        outputTextPane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return false;
            }
        };
    }
}
