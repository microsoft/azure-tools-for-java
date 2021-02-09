/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.handlers;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.AuthMethod;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

public class SignOutCommandHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        doSignOut(window.getShell());

        return null;
    }

    public static void doSignOut(Shell shell) {
        EventUtil.executeWithLog(TelemetryConstants.ACCOUNT, TelemetryConstants.SIGNOUT, (operation) -> {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            String artifact = (authMethodManager.getAuthMethod() == AuthMethod.AD
                || authMethodManager.getAuthMethod() == AuthMethod.DC)
                ? "Signed in as " + authMethodManager.getAuthMethodDetails().getAccountEmail()
                : "Signed in using file \"" + authMethodManager.getAuthMethodDetails().getCredFilePath() + "\"";
            MessageBox messageBox = new MessageBox(
                    shell,
                    SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setMessage(artifact + "\n"
                    + "Do you really want to sign out?");
            messageBox.setText("Azure Sign Out");


            int response = messageBox.open();
            if (response == SWT.YES) {
                if (authMethodManager.isSignedIn()) {
                    authMethodManager.signOut();
                }
            }
        }, (ex) -> ex.printStackTrace());
    }
}
