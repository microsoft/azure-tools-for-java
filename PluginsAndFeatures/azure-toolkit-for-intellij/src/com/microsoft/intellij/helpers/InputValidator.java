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

package com.microsoft.intellij.helpers;

import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.border.Border;

public abstract class InputValidator<T extends JComponent> extends InputVerifier {
    private Border originalBorder;

    @Override
    public final boolean verify(JComponent jComponent) {

        String error = validate((T) jComponent);

        jComponent.setToolTipText(error == null ? "" : error);
        if (originalBorder == null)
            originalBorder = jComponent.getBorder();

        jComponent.setBorder(error == null ? originalBorder : BorderFactory.createLineBorder(JBColor.RED));

        return (error == null);
    }

    @Override
    public final boolean shouldYieldFocus(JComponent jComponent) {
        super.shouldYieldFocus(jComponent);
        return true;
    }

    public abstract String validate(T component);
}
