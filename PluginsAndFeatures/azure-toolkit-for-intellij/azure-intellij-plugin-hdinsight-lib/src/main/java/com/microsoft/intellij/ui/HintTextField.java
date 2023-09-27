/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class HintTextField extends JBTextField {
    public HintTextField(@NotNull String hint) {
        super();
        getEmptyText().setText(hint);
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Read hint text when this component gets focused
                e.getComponent().getAccessibleContext().setAccessibleDescription(
                        StringUtils.isNoneEmpty(getText()) ? getText() : hint
                );
                super.focusGained(e);
            }
        });
    }
}
