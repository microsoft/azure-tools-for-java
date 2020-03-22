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
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.intellij.ui.components.Validatable;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class SelectLibraryStep extends AzureWizardStep<AddLibraryWizardModel> implements Validatable {
    private JPanel rootPanel;
    private JList libraryList;
    private final AddLibraryWizardModel myModel;

    public SelectLibraryStep(final String title, final AddLibraryWizardModel model) {
        super(title, message("selectLocationDesc"));
        myModel = model;
        init();
    }

    public void init() {
        libraryList.setListData(AzureLibrary.LIBRARIES);
        libraryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
        rootPanel.revalidate();
        return rootPanel;
    }


    @Override
    public WizardStep onNext(final AddLibraryWizardModel model) {
        if (doValidate() == null) {
            model.setSelectedLibrary((AzureLibrary) libraryList.getSelectedValue());
//            ((LibraryPropertiesStep) model.getNextFor(this)).setAzureLibrary((AzureLibrary) libraryList.getSelectedValue());
            return super.onNext(model);
        } else {
            return this;
        }
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

}

