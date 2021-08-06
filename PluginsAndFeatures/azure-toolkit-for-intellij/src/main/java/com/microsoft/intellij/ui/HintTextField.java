/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

public class HintTextField extends JBTextField {
    public HintTextField(@NotNull String hint) {
        super();
        getEmptyText().setText(hint);
    }
}
