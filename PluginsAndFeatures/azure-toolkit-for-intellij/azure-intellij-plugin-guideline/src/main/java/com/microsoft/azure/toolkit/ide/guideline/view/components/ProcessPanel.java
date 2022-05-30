package com.microsoft.azure.toolkit.ide.guideline.view.components;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ActionListener;

public class ProcessPanel extends JPanel {
    private ProcessConfig process;
    private JPanel pnlRoot;
    private JLabel lblIcon;
    private JLabel lblTitle;
    private JTextPane areaDescription;
    private JButton startButton;

    public ProcessPanel(@Nonnull ProcessConfig process) {
        super();
        this.process = process;
        $$$setupUI$$$();
        init();
    }

    public void setStartListener(@Nonnull final ActionListener listener) {
        this.startButton.addActionListener(listener);
    }

    private void init() {
        this.setLayout(new GridLayoutManager(1, 1));
        this.add(pnlRoot, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));
        this.lblTitle.setFont(JBFont.h4());
        this.startButton.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Action.START));
        // render process
        this.lblIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.lblTitle.setText(process.getTitle());
        this.areaDescription.setText(process.getDescription());
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }
}
