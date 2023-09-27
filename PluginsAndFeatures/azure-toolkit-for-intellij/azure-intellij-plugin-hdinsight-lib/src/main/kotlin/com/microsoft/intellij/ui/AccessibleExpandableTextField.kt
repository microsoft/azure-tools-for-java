/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.UIBundle
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class AccessibleExpandableTextField(val action: () -> String?): ExpandableTextField() {
    init {
        val actionWithTextChanged: () -> Unit = {
            val newText = action()
            if (newText != null) {
                text = newText
            }
        }
        this.addBrowseExtension(actionWithTextChanged, null)
    }

    override fun addBrowseExtension(action: Runnable, parentDisposable: Disposable?): ExtendableTextField? {
        val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK)
        val tooltip = UIBundle.message("component.with.browse.button.browse.button.tooltip.text") + " (" + KeymapUtil.getKeystrokeText(keyStroke) + ")"
        val browseExtension = ExtendableTextComponent.Extension.create(AllIcons.General.OpenDisk, AllIcons.General.OpenDiskHover, tooltip, action)
        object : DumbAwareAction() {
            override fun actionPerformed(e: AnActionEvent) {
                action.run()
            }
        }.registerCustomShortcutSet(CustomShortcutSet(keyStroke), this, parentDisposable)
        addExtension(browseExtension)
        return this
    }
}
