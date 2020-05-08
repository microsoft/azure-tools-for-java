/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.*
import com.jetbrains.rider.test.asserts.shouldBeFalse
import com.jetbrains.rider.test.asserts.shouldBeTrue
import org.testng.annotations.Test
import javax.swing.JPanel
import javax.swing.JToggleButton

class ComponentExtensionsTest {

    //region Enabled

    @Test
    fun testSetComponentsEnabled_Enable() {
        val textField = JBTextField().apply { isEnabled = false }
        textField.isEnabled.shouldBeFalse()

        val label = JBLabel().apply { isEnabled = false }
        label.isEnabled.shouldBeFalse()

        val comboBox = ComboBox<String>().apply { isEnabled = false }
        comboBox.isEnabled.shouldBeFalse()

        setComponentsEnabled(true, textField, label, comboBox)

        textField.isEnabled.shouldBeTrue()
        label.isEnabled.shouldBeTrue()
        comboBox.isEnabled.shouldBeTrue()
    }

    @Test
    fun testSetComponentsEnabled_Disable() {
        val textField = JBTextField()
        textField.isEnabled.shouldBeTrue()

        val label = JBLabel()
        label.isEnabled.shouldBeTrue()

        val comboBox = ComboBox<String>()
        comboBox.isEnabled.shouldBeTrue()

        setComponentsEnabled(false, textField, label, comboBox)

        textField.isEnabled.shouldBeFalse()
        label.isEnabled.shouldBeFalse()
        comboBox.isEnabled.shouldBeFalse()
    }

    //endregion Enabled

    //region Visible

    @Test
    fun testSetComponentsVisible_Visible() {
        val textField = JBTextField().apply { isVisible = false }
        textField.isVisible.shouldBeFalse()

        val label = JBLabel().apply { isVisible = false }
        label.isVisible.shouldBeFalse()

        val comboBox = ComboBox<String>().apply { isVisible = false }
        comboBox.isVisible.shouldBeFalse()

        setComponentsVisible(true, textField, label, comboBox)

        textField.isVisible.shouldBeTrue()
        label.isVisible.shouldBeTrue()
        comboBox.isVisible.shouldBeTrue()
    }

    @Test
    fun testSetComponentsVisible_NotVisible() {
        val textField = JBTextField()
        textField.isVisible.shouldBeTrue()

        val label = JBLabel()
        label.isVisible.shouldBeTrue()

        val comboBox = ComboBox<String>()
        comboBox.isVisible.shouldBeTrue()

        setComponentsVisible(false, textField, label, comboBox)

        textField.isVisible.shouldBeFalse()
        label.isVisible.shouldBeFalse()
        comboBox.isVisible.shouldBeFalse()
    }

    //endregion Visible

    //region Selected

    @Test
    fun testSetComponentsSelected_Selected() {
        val radioButton = JBRadioButton()
        radioButton.isSelected.shouldBeFalse()

        val checkBox = JBCheckBox()
        checkBox.isSelected.shouldBeFalse()

        val toggle = JToggleButton()
        toggle.isSelected.shouldBeFalse()

        setComponentsSelected(true, radioButton, checkBox, toggle)

        radioButton.isSelected.shouldBeTrue()
        checkBox.isSelected.shouldBeTrue()
        toggle.isSelected.shouldBeTrue()
    }

    @Test
    fun testSetComponentsSelected_NotSelected() {
        val radioButton = JBRadioButton().apply { isSelected = true }
        radioButton.isSelected.shouldBeTrue()

        val checkBox = JBCheckBox().apply { isSelected = true }
        checkBox.isSelected.shouldBeTrue()

        val toggle = JToggleButton().apply { isSelected = true }
        toggle.isSelected.shouldBeTrue()

        setComponentsSelected(false, radioButton, checkBox, toggle)

        radioButton.isSelected.shouldBeFalse()
        checkBox.isSelected.shouldBeFalse()
        toggle.isSelected.shouldBeFalse()
    }

    //endregion Selected

    //region Recursive Enable

    @Test
    fun testSetComponentEnabled_NonRecursively_Disable() {
        val textField = JBTextField()
        val panel = JPanel().apply {
            add(textField)
        }
        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeTrue()

        panel.setComponentEnabled(isEnabled = false, recursively = false)

        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeTrue()
    }

    @Test
    fun testSetComponentEnabled_NonRecursively_Enable() {
        val textField = JBTextField().apply { isEnabled = false }
        val panel = JPanel().apply {
            isEnabled = false
            add(textField)
        }
        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = true, recursively = false)

        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeFalse()
    }

    @Test
    fun testSetComponentEnabled_Recursively_Disable() {
        val textField = JBTextField()
        val panel = JPanel().apply {
            add(textField)
        }

        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeTrue()

        panel.setComponentEnabled(isEnabled = false, recursively = true)

        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeFalse()
    }

    @Test
    fun testSetComponentEnabled_Recursively_Enable() {
        val textField = JBTextField().apply { isEnabled = false }
        val panel = JPanel().apply {
            isEnabled = false
            add(textField)
        }
        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = true, recursively = true)

        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeTrue()
    }

    @Test
    fun testSetComponentEnabled_RecursivelyVisibleOnly_Disable() {
        val textField = JBTextField()
        val label = JBLabel().apply { isVisible = false }
        val panel = JPanel().apply {
            add(textField)
            add(label)
        }
        panel.isEnabled.shouldBeTrue()
        panel.isVisible.shouldBeTrue()

        textField.isEnabled.shouldBeTrue()
        textField.isVisible.shouldBeTrue()

        label.isEnabled.shouldBeTrue()
        label.isVisible.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = false, recursively = true, visibleOnly = true)

        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeFalse()
        label.isEnabled.shouldBeTrue()
    }

    @Test
    fun testSetComponentEnabled_RecursivelyVisibleOnly_Enable() {
        val textField = JBTextField().apply { isEnabled = false }
        val label = JBLabel().apply { isEnabled = false; isVisible = false }
        val panel = JPanel().apply {
            add(textField)
            add(label)
            isEnabled = false
        }
        panel.isEnabled.shouldBeFalse()
        panel.isVisible.shouldBeTrue()

        textField.isEnabled.shouldBeFalse()
        textField.isVisible.shouldBeTrue()

        label.isEnabled.shouldBeFalse()
        label.isVisible.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = true, recursively = true, visibleOnly = true)

        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeTrue()
        label.isEnabled.shouldBeFalse()
    }

    @Test
    fun testSetComponentEnabled_RecursivelyAll_Disable() {
        val textField = JBTextField()
        val label = JBLabel().apply { isVisible = false }
        val panel = JPanel().apply {
            add(textField)
            add(label)
        }
        panel.isEnabled.shouldBeTrue()
        panel.isVisible.shouldBeTrue()

        textField.isEnabled.shouldBeTrue()
        textField.isVisible.shouldBeTrue()

        label.isEnabled.shouldBeTrue()
        label.isVisible.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = false, recursively = true, visibleOnly = false)

        panel.isEnabled.shouldBeFalse()
        textField.isEnabled.shouldBeFalse()
        label.isEnabled.shouldBeFalse()
    }

    @Test
    fun testSetComponentEnabled_RecursivelyAll_Enable() {
        val textField = JBTextField().apply { isEnabled = false }
        val label = JBLabel().apply { isEnabled = false; isVisible = false }
        val panel = JPanel().apply {
            add(textField)
            add(label)
            isEnabled = false
        }
        panel.isEnabled.shouldBeFalse()
        panel.isVisible.shouldBeTrue()

        textField.isEnabled.shouldBeFalse()
        textField.isVisible.shouldBeTrue()

        label.isEnabled.shouldBeFalse()
        label.isVisible.shouldBeFalse()

        panel.setComponentEnabled(isEnabled = true, recursively = true, visibleOnly = false)

        panel.isEnabled.shouldBeTrue()
        textField.isEnabled.shouldBeTrue()
        label.isEnabled.shouldBeTrue()
    }

    //endregion Recursive Enable
}
