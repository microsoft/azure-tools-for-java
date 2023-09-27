/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui

import com.microsoft.azure.hdinsight.common.DarkThemeManager
import org.apache.commons.lang3.StringUtils
import javax.swing.JTextArea

class ErrorTextArea : JTextArea() {
    override fun setText(text: String?) {
        foreground = DarkThemeManager.getInstance().errorMessageColor
        background = if (StringUtils.isEmpty(text)) {
            null
        } else {
            DarkThemeManager.getInstance().errorTextBackgroundColor
        }

        super.setText(text)
    }
}
