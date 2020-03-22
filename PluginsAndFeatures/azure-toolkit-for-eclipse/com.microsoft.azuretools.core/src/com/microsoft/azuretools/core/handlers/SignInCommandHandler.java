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
