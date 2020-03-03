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

package com.microsoft.azuretools.ijidea.ui

import com.intellij.ui.HideableTitledPanel
import com.intellij.util.ui.JBUI
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.accessibility.AccessibleAction
import javax.accessibility.AccessibleContext
import javax.swing.JComponent
import javax.swing.UIManager

class AccessibleHideableTitledPanel(
        title: String,
        val content: JComponent,
        val canAdjustWindow: Boolean = false,
        val isStartedOn: Boolean = false
) : HideableTitledPanel(title, canAdjustWindow, content, isStartedOn), AccessibleAction {
    override fun getAccessibleActionDescription(i: Int): String? {
        if (i < 0 || i >= accessibleActionCount) {
            return null
        }

        if (i == 0 || !isExpanded) {
            return AccessibleAction.TOGGLE_EXPAND
        }

        return this.content.accessibleContext.accessibleAction?.getAccessibleActionDescription(i - 1)
    }

    override fun getAccessibleActionCount(): Int =
            if (isExpanded) {
                (this.content.accessibleContext.accessibleAction?.accessibleActionCount ?: 0) + 1
            } else 1

    override fun doAccessibleAction(i: Int): Boolean {
        if (i < 0 || i >= accessibleActionCount) {
            return false
        }

        if (i == 0) {
            setOn(!isExpanded)

            return true
        }

        if (!isExpanded) {
            return false
        }

        return this.content.accessibleContext.accessibleAction?.doAccessibleAction(i - 1) ?: false
    }

    init {
        isFocusable = true
        name = title

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                border = null
            }

            override fun focusGained(e: FocusEvent?) {
                border = JBUI.Borders.customLine(UIManager.getColor("TextField.focusedBorderColor"))
            }
        })

        addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_SPACE) {
                    setOn(!isExpanded)
                }
            }
        })
    }

    private val hideableAccessibleContext: AccessibleContext by lazy {
        object : AccessibleJPanel() {
            init {
                accessibleName = title
                accessibleDescription = "Press SPACE button to toggle the hideable panel"
            }

            override fun getAccessibleAction() = this@AccessibleHideableTitledPanel
        }
    }

    override fun getAccessibleContext(): AccessibleContext = hideableAccessibleContext
}