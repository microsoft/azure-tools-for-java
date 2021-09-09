/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * UI used by {@code com.microsoft.azure.toolkit.intellij.connector.aad.RegisterApplicationAction}.
 */
class RegisterApplicationInAzureAdDialog extends AzureDialog<ApplicationRegistrationModel> {
    private final RegisterAzureApplicationForm form = new RegisterAzureApplicationForm();

    RegisterApplicationInAzureAdDialog(@NotNull Project project, @NotNull ApplicationRegistrationModel data) {
        super(project);
        form.setData(data);
        init();
    }

    @Override
    public AzureForm<ApplicationRegistrationModel> getForm() {
        return form;
    }

    @Override
    protected String getDialogTitle() {
        return MessageBundle.message("registerAppDialog.title");
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return form.getContentPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return form.getPreferredFocusedComponent();
    }
}
