/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

@Getter
public class TestConnectionActionPanel extends JPanel {
    private AzureActionButton<Void> copyButton;
    private JLabel iconLabel;
    private JPanel root;

    private final Border rolloverBorder;
    private final Border emptyBorder;

    public TestConnectionActionPanel() {
        super();
        rolloverBorder = copyButton.getBorder();
        emptyBorder = BorderFactory.createEmptyBorder();
        copyButton.setBorder(emptyBorder);
        copyButton.setContentAreaFilled(false);
        copyButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                if (copyButton.isRolloverEnabled()) {
                    copyButton.setBorder(rolloverBorder);
                    copyButton.setContentAreaFilled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (copyButton.isRolloverEnabled()) {
                    copyButton.setBorder(emptyBorder);
                    copyButton.setContentAreaFilled(false);
                }
            }
        });
        iconLabel.setIcon(AllIcons.General.InspectionsOK);
        copyButton.setIcon(AllIcons.General.CopyHovered);
    }

    @Override
    public void setVisible(boolean visible) {
        root.setVisible(visible);
    }
}
