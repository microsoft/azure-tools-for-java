/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui.libraries;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardModel;
import com.microsoft.intellij.ui.components.Validatable;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AddLibraryWizardModel extends WizardModel {
    private SelectLibraryStep selectLibraryStep;
    private LibraryPropertiesStep libraryPropertiesStep;
    private Module myModule;
    private AzureLibrary selectedLibrary;
    private boolean exported;

    public AddLibraryWizardModel(final Module module) {
        super(message("addLibraryTitle"));
        myModule = module;
        selectLibraryStep = new SelectLibraryStep(this.getTitle(), this);
        libraryPropertiesStep = new LibraryPropertiesStep(this.getTitle(), this);
        add(selectLibraryStep);
        add(libraryPropertiesStep);
    }

    public void setSelectedLibrary(AzureLibrary selectedLibrary) {
        this.selectedLibrary = selectedLibrary;
    }

    public AzureLibrary getSelectedLibrary() {
        return selectedLibrary;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public boolean isExported() {
        return exported;
    }

    public Module getMyModule() {
        return myModule;
    }

    public ValidationInfo doValidate() {
        return ((Validatable) getCurrentStep()).doValidate();
    }
}
