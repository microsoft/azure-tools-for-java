/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
