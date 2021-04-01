/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import javax.swing.*;

public class SwingUtils {
    public static void setTextAndEnableAutoWrap(JLabel label, String text) {
        label.setText("<html><body>" + text + "</body></html>");
    }
}
