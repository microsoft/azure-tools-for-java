/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.ui;

import javax.swing.*;
import java.util.Objects;

public class TextFieldUtils {

    public static void disableTextBoard(JTextField... fields) {
        if (Objects.isNull(fields)) {
            return;
        }
        for (JTextField field : fields) {
            field.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    public static void makeTextOpaque(JTextField ... fields) {
        if (Objects.isNull(fields)) {
            return;
        }
        for (JTextField field : fields) {
            field.setBackground(null);
        }
    }
}
