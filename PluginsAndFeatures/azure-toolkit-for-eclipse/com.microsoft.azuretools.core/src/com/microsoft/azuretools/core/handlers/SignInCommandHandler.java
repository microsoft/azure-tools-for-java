/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.AuthMethodDetails;
import com.microsoft.azuretools.core.ui.SignInDialog;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

public class SignInCommandHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        doSignIn(window.getShell());

        return null;
    }

    public static boolean doSignIn(Shell shell) {
        try {
            AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
            boolean isSignIn = authMethodManager.isSignedIn();
            if (isSignIn) return true;
            SignInDialog d = SignInDialog.go(shell, authMethodManager.getAuthMethodDetails());
            if (null != d) {
                AuthMethodDetails authMethodDetailsUpdated = d.getAuthMethodDetails();
                authMethodManager.setAuthMethodDetails(authMethodDetailsUpdated);
                SelectSubsriptionsCommandHandler.onSelectSubscriptions(shell);
                authMethodManager.notifySignInEventListener();
            }
            return authMethodManager.isSignedIn();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
