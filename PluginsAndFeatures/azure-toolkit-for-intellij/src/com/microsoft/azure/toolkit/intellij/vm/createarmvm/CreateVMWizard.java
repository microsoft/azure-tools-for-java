/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.createarmvm;

import com.intellij.ui.wizard.WizardDialog;
import com.microsoft.azure.toolkit.intellij.vm.VMWizardModel;
import com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule;

import javax.swing.*;
import java.awt.*;

public class CreateVMWizard extends WizardDialog<VMWizardModel> {
    public CreateVMWizard(VMArmModule node) {
        super(true, true, new VMWizardModel(node));
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        this.getWindow();
        this.setResizable(false);
        return new Dimension(500, 467);
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanelComp = super.createSouthPanel();

        if (southPanelComp instanceof JPanel) {
            final JPanel southPanel = (JPanel) southPanelComp;

            if (southPanel.getComponentCount() == 1 && southPanel.getComponent(0) instanceof JPanel) {
                JPanel panel = (JPanel) southPanel.getComponent(0);

                for (Component buttonComp : panel.getComponents()) {
                    if (buttonComp instanceof JButton) {
                        JButton button = (JButton) buttonComp;
                        String text = button.getText();

                        if (text != null) {
                            if (text.equals("Help")) {
                                panel.remove(button);
                            }
                        }
                    }
                }
            }
        }
        return southPanelComp;
    }
}

