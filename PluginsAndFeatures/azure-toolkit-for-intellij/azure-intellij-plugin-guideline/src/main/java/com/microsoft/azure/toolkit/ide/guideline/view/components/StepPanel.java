package com.microsoft.azure.toolkit.ide.guideline.view.components;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guideline.Step;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.annotation.Nonnull;
import javax.swing.*;

public class StepPanel extends JPanel {
    private JLabel lblStatusIcon;
    private JLabel lblTitle;
    private JButton runButton;
    private JPanel pnlInputs;
    private JPanel pnlOutputs;
    private JPanel pnlRoot;

    private Step step;

    public StepPanel(@Nonnull final Step step) {
        super();
        this.step = step;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.setLayout(new GridLayoutManager(1, 1));
        this.add(pnlRoot, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0));
        renderStep();
    }

    private void renderStep() {
        this.lblStatusIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.lblTitle.setText(step.getTitle());

        runButton.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.START));
        runButton.addActionListener(e -> step.executeWithUI(step.getPhase().getProcess().getContext()));
    }

    void $$$setupUI$$$() {
    }
}
