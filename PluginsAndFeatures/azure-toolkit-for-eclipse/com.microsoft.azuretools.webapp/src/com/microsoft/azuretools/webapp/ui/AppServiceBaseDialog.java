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
package com.microsoft.azuretools.webapp.ui;

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
