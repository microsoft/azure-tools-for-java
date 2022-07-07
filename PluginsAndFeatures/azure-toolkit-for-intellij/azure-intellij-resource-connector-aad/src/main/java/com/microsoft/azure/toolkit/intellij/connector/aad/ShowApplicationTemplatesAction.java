/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.lib.Azure.az;

/**
 * Displays UI to display the code templates for the registered Azure AD applications.
 * <p>
 * ComponentNotRegistered is suppressed, because IntelliJ isn't finding the reference in resources/META-INF.
 */
@SuppressWarnings("ComponentNotRegistered")
public class ShowApplicationTemplatesAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ShowApplicationTemplatesAction.class);

    public ShowApplicationTemplatesAction() {
        super(MessageBundle.message("action.AzureToolkit.AD.AzureAppTemplates.text"));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        try {
            // throws an exception if user is not signed in
            az(AzureAccount.class).account();
        } catch (AzureToolkitAuthenticationException ex) {
            LOG.debug("user is not signed in", ex);
            e.getPresentation().setEnabled(false);
        }
    }

    @Override
    @ExceptionNotification
    @AzureOperation(name = "aad.show_application_templates", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@Nonnull AnActionEvent e) {
        var project = e.getProject();
        assert project != null;

        new AzureApplicationTemplateDialog(project, null).show();
    }
}
