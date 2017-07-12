package com.microsoft.intellij.container.ui;

import com.intellij.ui.wizard.WizardModel;

/**
 * Created by yanzh on 7/11/2017.
 */
public class PublishWizardModel extends WizardModel{
    private static final String TEXT_WINDOW_TITLE = "Publish to Web App on Linux";
    private PublishWizardDialog dialog;
    public PublishWizardModel() {
        super(TEXT_WINDOW_TITLE);
        this.add(new StepOnePage(this));
        this.add(new StepTwoPage(this));
    }

    public PublishWizardDialog getDialog() {
        return dialog;
    }

    public void setDialog(PublishWizardDialog dialog) {
        this.dialog = dialog;
    }
}
