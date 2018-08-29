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

package com.microsoft.azure.sparkserverless.common;

import com.intellij.ui.JBColor;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;
import java.awt.*;

public class ErrorMessageTooltip extends JToolTip{
    @Nullable
    private Popup popup;

    public ErrorMessageTooltip(@NotNull JComponent component) {
        super();
        setComponent(component);
        setForeground(JBColor.RED);
    }

    public void hideToolTip() {
        if (popup != null) {
            popup.hide();
        }
    }

    public void showErrorToolTip(@NotNull Point location, @Nullable String errorMsg) {
        try {
            setTipText(errorMsg);
            popup = PopupFactory.getSharedInstance().getPopup(getComponent(), this, location.x, location.y - 20);
            popup.show();
        } catch (IllegalArgumentException ex) {
            // This exception happens when ErrorMessageTooltip is not initialized
            assert this != null : "Unreachable Code Error";
        }
    }

    public void setVisible(@NotNull Validatable validator) {
        getComponent().putClientProperty("JComponent.outline", validator.isLegal() ? null : "error");

        // Clean up previous error tooltip whether validator is legal or not
        // If we move hideTooltip() into else, error tooltips might overlap for one component
        hideToolTip();

        if (!validator.isLegal() && getComponent().isShowing()) {
            showErrorToolTip(getComponent().getLocationOnScreen(), validator.getErrorMessage());
        }
    }
}
