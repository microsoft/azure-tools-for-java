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
