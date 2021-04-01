/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.Activator;

/**
 * Class has methods to display and log the error.
 */
public class MessageUtil {

    /**
     * This method will display the error message box
     * when any error occurs.
     *
     * @param shell       parent shell
     * @param title       the text or title of the window.
     * @param message     the message which is to be displayed
     */
    public static void displayErrorDialog(Shell shell,
            String title, String message) {
         MessageDialog.openError(shell, title, message);
    }

    /**
     * This method will display the error message box
     * when any error occurs and also logs error.
     * @param shell
     * @param title : Error title
     * @param message : Error message
     * @param e : exception
     */
    public static void displayErrorDialogAndLog(Shell shell,
            String title, String message,
            Exception e) {
        Activator.getDefault().log(message, e);
        displayErrorDialog(shell, title, message);
    }
}
