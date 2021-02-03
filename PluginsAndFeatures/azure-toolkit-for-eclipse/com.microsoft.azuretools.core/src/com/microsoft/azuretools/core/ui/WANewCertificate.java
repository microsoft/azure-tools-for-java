/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.ui.commoncontrols.Messages;
import com.microsoft.azuretools.core.ui.commoncontrols.NewCertificateDialog;
import com.microsoft.azuretools.core.ui.commoncontrols.NewCertificateDialogData;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;

/**
 * This class creates new self signed certificates.
 */
public class WANewCertificate extends AzureAbstractHandler {

    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        try {
            NewCertificateDialogData data = new NewCertificateDialogData();
            /*
             * third parameter is jdkPath
             * as its toolbar button, do not refer any project for JDK path
             * just pass empty string.
             */
            NewCertificateDialog dialog = new NewCertificateDialog(PluginUtil.getParentShell(), data, "");
            // Open the dialog
            dialog.open();
        } catch (Exception e) {
            PluginUtil.displayErrorDialogAndLog(PluginUtil.getParentShell(), Messages.newCertDlgCrtErTtl,
                                                Messages.newCertMsg, e);
            Activator.getDefault().log(Messages.newCertMsg, e);
        }
        return null;
    }
}
