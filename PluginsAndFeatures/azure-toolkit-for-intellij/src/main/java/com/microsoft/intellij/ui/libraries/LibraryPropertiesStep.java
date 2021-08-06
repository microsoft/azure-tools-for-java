/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
