/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui

import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.ui.JBUI
import javax.swing.JList

abstract class AccessibleListCellRenderer<T>() : SimpleListCellRenderer<T>() {
    override fun customize(list: JList<out T>, value: T, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = getText(value)

        if (selected) {
            background = JBUI.CurrentTheme.List.Selection.background(hasFocus)
            foreground = JBUI.CurrentTheme.List.Selection.foreground(hasFocus)
        }
    }

    abstract fun getText(value: T): String
}