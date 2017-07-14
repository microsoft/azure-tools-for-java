/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardDialog;

import javax.swing.*;
import java.awt.*;

public class PublishWizardDialog extends WizardDialog<PublishWizardModel> {
    private JComponent southPanelComponent;

    public PublishWizardDialog(boolean canBeParent, boolean tryApplicationModal, PublishWizardModel model) {
        super(canBeParent, tryApplicationModal, model);
        model.setDialog(this);
        this.doOKAction();
    }

    @Override
    protected JComponent createSouthPanel() {
        JComponent southPanelComp = super.createSouthPanel();
        this.southPanelComponent = southPanelComp;
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

//    public JComponent getSouthPanelComponent() {
//        return southPanelComponent;
//    }
//
//    public JButton getPrevButton(){
//        return (JButton) ((JPanel)southPanelComponent.getComponent(0)).getComponent(0);
//    }
//
//    public JButton getNextButton(){
//        return (JButton) ((JPanel)southPanelComponent.getComponent(0)).getComponent(1);
//    }
//
//    public JButton getFinishButton(){
//        return (JButton) ((JPanel)southPanelComponent.getComponent(0)).getComponent(2);
//    }
//
//    public JButton getCancelButton(){
//        return (JButton) ((JPanel)southPanelComponent.getComponent(0)).getComponent(3);
//    }
}
