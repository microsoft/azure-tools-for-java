/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azuretools.core.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.microsoft.azuretools.authmanage.AdAuthManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.interact.AuthMethod;
import com.microsoft.azuretools.core.utils.AzureAbstractHandler;

public class SignOutCommandHandler extends AzureAbstractHandler {

    @Override
    public Object onExecute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        doSignOut(window.getShell());

        return null;
    }

	public static void doSignOut(Shell shell) {
		try {
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
                AdAuthManager adAuthManager = AdAuthManager.getInstance();
                if (adAuthManager.isSignedIn()) {
                    adAuthManager.signOut();
                }
                authMethodManager.signOut();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
}
