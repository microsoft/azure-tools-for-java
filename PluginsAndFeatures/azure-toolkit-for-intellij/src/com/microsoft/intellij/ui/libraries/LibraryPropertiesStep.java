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
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.intellij.ui.components.Validatable;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class LibraryPropertiesStep extends AzureWizardStep<AddLibraryWizardModel> implements Validatable {

    private LibraryPropertiesPanel libraryPropertiesPanel;
    private final AddLibraryWizardModel myModel;

    public LibraryPropertiesStep(String title, final AddLibraryWizardModel model) {
        super(title, message("libraryPropertiesDesc"));
        myModel = model;
    }

    @Override
    public JComponent prepare(final WizardNavigationState state) {
        libraryPropertiesPanel = new LibraryPropertiesPanel(myModel.getMyModule(), myModel.getSelectedLibrary(), false, true);
        return libraryPropertiesPanel.prepare();
    }

    @Override
    public ValidationInfo doValidate() {
        return null;
    }

    @Override
    public boolean onFinish() {
        boolean result = libraryPropertiesPanel.onFinish();
        if (result) {
            myModel.setExported(libraryPropertiesPanel.isExported());
            return super.onFinish();
        }
        return false;
    }
}
