/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ide.CopyPasteManager;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.database.entity.IDatabase;
import lombok.Getter;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;

public class ConnectionStringsOutputPanel extends JPanel {
    @Getter
    private JTextArea outputTextArea;
    private JPanel rootPanel;
    @Getter
    private AzureActionButton<IDatabase> copyButton;
    @Getter
    private JLabel titleLabel;
    private JLabel lblWarning;
    @Getter
    private JTextPane outputTextPane;

    public ConnectionStringsOutputPanel() {
        super();
        $$$setupUI$$$();
        this.lblWarning.setIcon(AllIcons.General.BalloonWarning);
        this.copyButton.setIcon(AllIcons.General.CopyHovered);
        final Action<IDatabase> copyAction = new Action<IDatabase>(Action.Id.of("user/$database.copy_connection_string"))
                .withAuthRequired(false)
                .withHandler(ignore -> CopyPasteManager.getInstance().setContents(new StringSelection(outputTextPane.getText())));
        this.copyButton.setAction(copyAction);
    }

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

    void $$$setupUI$$$() {
    }
}
