package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardDialog;

import javax.swing.*;
import java.awt.*;

/**
 * Created by yanzh on 7/11/2017.
 */
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
