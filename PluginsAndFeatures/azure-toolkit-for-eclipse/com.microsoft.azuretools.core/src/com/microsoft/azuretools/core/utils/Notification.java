/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.authmanage.interact.INotification;

public class Notification implements INotification {

    @Override
    public void deliver(String subject, String message) {
        Shell shell = getShell();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                MessageDialog.openInformation(
                        shell,
                        subject,
                        message);
            }
        });
    }

    private Shell getShell() {
        Display display = Display.getCurrent();
        //may be null if outside the UI thread
        if (display == null)
           display = Display.getDefault();
        Shell shell = display.getActiveShell();

        if (shell == null) {
            shell = new Shell();
        }
        return shell;
    }
}
