/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.appservice.ui;

import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public abstract class AppServiceBaseDialog extends AzureTitleAreaDialogWrapper {

    private List<ControlDecoration> decorations = new LinkedList<>();
    private static final String FORM_VALIDATION_ERROR = "Validation error: %s";

    public AppServiceBaseDialog(Shell parentShell) {
        super(parentShell);
    }

    protected ControlDecoration decorateContorolAndRegister(Control c) {
        ControlDecoration d = new ControlDecoration(c, SWT.TOP | SWT.LEFT);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
            .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        Image img = fieldDecoration.getImage();
        d.setImage(img);
        d.hide();
        decorations.add(d);
        return d;
    }

    protected void setError(ControlDecoration d, String message) {
        Display.getDefault().asyncExec(() -> {
            d.setDescriptionText(message);
            setErrorMessage(String.format(FORM_VALIDATION_ERROR, message));
            d.show();
        });
    }

    protected void cleanError() {
        for (ControlDecoration d : decorations) {
            d.hide();
        }
        setErrorMessage(null);
    }
}
