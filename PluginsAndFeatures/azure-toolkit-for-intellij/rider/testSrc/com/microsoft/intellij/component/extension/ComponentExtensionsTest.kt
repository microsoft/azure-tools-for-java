package com.microsoft.intellij.component.extension

import org.testng.annotations.Test
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

class ComponentExtensionsTest {

    @Test
    fun testDisableAllEnabledComponents() {
        val pnlMain = JPanel()

        val textField = JTextField()
        textField.isEnabled = true

        val comboBox = JComboBox<String>()
        comboBox.isEnabled = true

        pnlMain.add(textField)
        pnlMain.add(comboBox)

        pnlMain.setComponentEnabled(false, pnlMain)
    }

}