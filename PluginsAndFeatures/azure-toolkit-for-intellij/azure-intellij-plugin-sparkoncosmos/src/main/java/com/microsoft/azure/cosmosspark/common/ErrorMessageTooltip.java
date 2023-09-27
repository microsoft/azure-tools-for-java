/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.common;

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
