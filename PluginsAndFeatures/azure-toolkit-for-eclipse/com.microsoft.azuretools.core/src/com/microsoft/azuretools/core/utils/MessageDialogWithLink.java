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
package com.microsoft.azuretools.core.utils;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

import com.microsoft.azuretools.core.Activator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

public class MessageDialogWithLink extends MessageDialog {

    public MessageDialogWithLink(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, String messageWithLink,
            int dialogImageType, int defaultIndex, String... okLabel) {
        super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, okLabel, defaultIndex);

        this.messageWithLink = messageWithLink;
    }

    public static boolean open(int kind, Shell parent, String title, String message, String messageWithLink, int style) {
        MessageDialogWithLink dialog = new MessageDialogWithLink(parent, title, null, message, messageWithLink, kind, 0, IDialogConstants.OK_LABEL);
        style &= SWT.SHEET;
        dialog.setShellStyle(dialog.getShellStyle() | style);
        return dialog.open() == 0;
    }

    public static void openError(Shell parent, String title, String message, String messageWithLink) {
        open(MessageDialog.ERROR, parent, title, message, messageWithLink, SWT.NONE);
    }

    public static void openInformation(Shell parent, String title, String message, String messageWithLink) {
        open(MessageDialog.INFORMATION, parent, title, message, messageWithLink, SWT.NONE);
    }

    protected Control createCustomArea(Composite parent) {
        Link link = new Link(parent, SWT.WRAP);
        link.setText(messageWithLink);
        link.addListener(SWT.Selection, e -> {
            openURL(e.text);
        });

        return link;
    }

    private String messageWithLink;

    private static void openURL(String url) {
        Desktop dt = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (dt != null && dt.isSupported(Desktop.Action.BROWSE)) {
            try {
                dt.browse(new URI(url));
            } catch (Exception ignore) {
                Activator.getDefault().log(ignore.getMessage(), ignore);
            }
        }
    }
}
