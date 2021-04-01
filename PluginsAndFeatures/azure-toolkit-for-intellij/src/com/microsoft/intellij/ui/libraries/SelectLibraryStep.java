/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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

