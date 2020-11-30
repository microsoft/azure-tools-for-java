/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.forms.appservice.slot

import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.AzureResourceNameComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import javax.swing.JLabel
import javax.swing.JPanel

abstract class CreateDeploymentSlotComponent(private val app: WebAppBase,
                                             private val isLoadFinishedSignal: Signal<Boolean>) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")),
        AzureComponent {

    companion object {
        private val doNotCloneSettingsName = RiderAzureBundle.message("dialog.create_deployment_slot.existing_settings.do_not_clone_settings")
    }

    val pnlName = AzureResourceNameComponent()

    private val pnlDeploymentSlotSettings =
            JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]"))

    private val lblCloneSettings =
            JLabel(RiderAzureBundle.message("dialog.create_deployment_slot.settings_clone.label"))

    val cbExistingSettings = object : AzureComboBox<String>() {
        override fun loadItems(): List<String> {
            val slotNames = loadSlotNamesAction()
            isLoadFinishedSignal.fire(true)

            return listOf(doNotCloneSettingsName, app.name()) + slotNames
        }
    }

    val slotName: String
        get() = pnlName.txtNameValue.text

    val isCloneSettings: Boolean
        get() = cbExistingSettings.getSelectedValue() != doNotCloneSettingsName

    init {
        pnlDeploymentSlotSettings.apply {
            add(lblCloneSettings)
            add(cbExistingSettings, "growx")
        }

        apply {
            add(pnlName, "growx")
            add(pnlDeploymentSlotSettings, "growx")
        }
    }

    abstract fun loadSlotNamesAction(): List<String>
}
