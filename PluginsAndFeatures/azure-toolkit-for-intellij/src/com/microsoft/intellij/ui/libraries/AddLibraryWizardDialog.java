/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardDialog;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class AddLibraryWizardDialog extends WizardDialog<AddLibraryWizardModel> {

    private AddLibraryWizardModel model;
    //todo:
    private String errorTitle;
    private String errorMessage;

    public AddLibraryWizardDialog(AddLibraryWizardModel model) {
        super(model.getMyModule().getProject(), true, model);
        this.model = model;
    }

    @Override
    public void onWizardGoalAchieved() {
        super.onWizardGoalAchieved();
    }

    @Override
    protected Dimension getWindowPreferredSize() {
        return new Dimension(400, 400);
    }

    public void setCancelText(String text) {
        myModel.getCurrentNavigationState().CANCEL.setName(text);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        return myModel.doValidate();
    }

    protected boolean postponeValidation() {
        return false;
    }

    @Override
    protected void doOKAction() {
//        validateInput();
        if (isOKActionEnabled() && performFinish()) {
            super.doOKAction();
        }
    }

    /**
     * This method gets called when wizard's finish button is clicked.
     *
     * @return True, if project gets created successfully; else false.
     */
    private boolean performFinish() {
        return true;
    }
}
