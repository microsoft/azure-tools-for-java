/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzureSdkReferenceBookDialog extends DialogWrapper {
    private final AzureSdkReferenceBookPanel bookPanel;

    protected AzureSdkReferenceBookDialog(@Nullable final Project project) {
        super(project);
        this.bookPanel = new AzureSdkReferenceBookPanel();
        this.setTitle("Azure SDK Reference Book");
        this.setModal(true);
        this.init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.bookPanel.getContentPanel();
    }
}
