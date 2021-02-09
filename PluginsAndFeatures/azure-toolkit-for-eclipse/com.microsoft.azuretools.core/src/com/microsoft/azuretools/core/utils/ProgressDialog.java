/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;

public class ProgressDialog extends ProgressMonitorDialog {
    private String title;

    public static ProgressDialog get(Shell parent, String title) {
        return new ProgressDialog(parent, title);
    }

    private ProgressDialog(Shell parent, String title) {
        super(parent);
        this.title = title;
    }

    @Override
    protected void configureShell(final Shell shell)
    {
        super.configureShell(shell);
        if (title != null ) shell.setText(title);
    }
}
