/**
 * Copyright (c) 2018 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.extension

import com.intellij.util.ui.UIUtil
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.util.containers.JBIterable
import java.awt.Component
import javax.swing.*

private const val COMPONENT_ENABLED_STATE_PROPERTY_NAME = "ComponentEnabledState"

fun setComponentsEnabled(isEnabled: Boolean, vararg components: JComponent) {
    components.forEach { it.isEnabled = isEnabled }
}
fun setComponentsVisible(isVisible: Boolean, vararg components: JComponent) {
    components.forEach { it.isVisible = isVisible }
}

fun setComponentsSelected(isSelected: Boolean, vararg components: JToggleButton) {
    components.forEach { it.isSelected = isSelected }
}

fun JComponent.setComponentEnabled(isEnabled: Boolean, recursively: Boolean = true, visibleOnly: Boolean = true) {
    val all =
            if (recursively)
                UIUtil.uiTraverser(this).expandAndFilter(
                        if (visibleOnly) Condition<Component> { it.isVisible }
                        else Conditions.alwaysTrue()
                ).traverse()
            else
                JBIterable.of(this)

    val fg = if (isEnabled) UIUtil.getLabelForeground() else UIUtil.getLabelDisabledForeground()

    for (c in all) {
        val comp = c as? JComponent ?: continue

        if (isEnabled) {
            if (comp.getClientProperty(COMPONENT_ENABLED_STATE_PROPERTY_NAME) != null &&
                    comp.getClientProperty(COMPONENT_ENABLED_STATE_PROPERTY_NAME) as? Boolean == false)
                continue
        } else {
            comp.putClientProperty(COMPONENT_ENABLED_STATE_PROPERTY_NAME, c.isEnabled)
        }

        c.isEnabled = isEnabled
        if (c is JLabel) {
            c.setForeground(fg)
        }
    }
}
