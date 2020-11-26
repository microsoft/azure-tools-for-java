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

package com.microsoft.intellij.ui.component.appservice

import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rd.util.getOrCreate
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rd.util.reactive.Property
import com.microsoft.azure.management.appservice.DeploymentSlot
import com.microsoft.azure.management.appservice.DeploymentSlotBase
import com.microsoft.azure.management.appservice.FunctionDeploymentSlot
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.helpers.validator.FunctionAppValidator
import com.microsoft.intellij.helpers.validator.WebAppValidator
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import net.miginfocom.swing.MigLayout
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import javax.swing.JPanel

class FunctionDeploymentSlotPublishSettingsPanel(lifetime: Lifetime) :
        DeploymentSlotPublishSettingsPanelBase<FunctionDeploymentSlot>(lifetime) {
    override fun getSlotsAction(subscriptionId: String, appId: String): List<FunctionDeploymentSlot> =
            AzureFunctionAppMvpModel.listDeploymentSlots(subscriptionId, appId, true)

    override fun validateComponent(): List<ValidationInfo> =
            super.validateComponent() + listOfNotNull(
                    if (isDeployEnabled)
                        FunctionAppValidator.checkDeploymentSlotIsSet(cbDeploymentSlots.getSelectedValue())
                                .toValidationInfo(cbDeploymentSlots)
                    else null
            )
}

class DeploymentSlotPublishSettingsPanel(lifetime: Lifetime) :
        DeploymentSlotPublishSettingsPanelBase<DeploymentSlot>(lifetime) {
    override fun getSlotsAction(subscriptionId: String, appId: String): List<DeploymentSlot> =
            AzureWebAppMvpModel.getInstance().getDeploymentSlots(subscriptionId, appId)

    override fun validateComponent(): List<ValidationInfo> =
            super.validateComponent() + listOfNotNull(
                    if (isDeployEnabled)
                        WebAppValidator.checkDeploymentSlotIsSet(cbDeploymentSlots.getSelectedValue())
                                .toValidationInfo(cbDeploymentSlots)
                    else null
            )
}

abstract class DeploymentSlotPublishSettingsPanelBase<T: DeploymentSlotBase<T>>(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    private val appToSlotsMap = concurrentMapOf<WebAppBase, List<T>>()

    val appProperty: IProperty<WebAppBase?> = Property(null)

    val checkBoxIsEnabled: JCheckBox = JCheckBox("Deploy to slot:", false)

    abstract fun getSlotsAction(subscriptionId: String, appId: String): List<T>
    
    val isDeployEnabled: Boolean
        get() = checkBoxIsEnabled.isSelected

    val cbDeploymentSlots = object : AzureComboBox<T>() {

        override fun loadItems(): List<T> {
            val app = appProperty.value
            if (app == null) {
                checkBoxIsEnabled.isEnabled = false
                this.isEnabled = false
                return emptyList()
            }

            val slots = appToSlotsMap.getOrCreate(app) {
                val subscriptionId = app.manager().subscriptionId()
                val appId = app.id()
                getSlotsAction(subscriptionId, appId)
            }

            checkBoxIsEnabled.isEnabled = slots.isNotEmpty()
            this.isEnabled = slots.isNotEmpty()

            return slots
        }

        override fun getItemText(item: Any?): String =
                (item as? DeploymentSlotBase<*>)?.name() ?: super.getItemText(item)

        override fun setEnabled(enabled: Boolean) {
            if (!checkBoxIsEnabled.isSelected) {
                super.setEnabled(false)
                return
            }

            super.setEnabled(enabled)
        }
    }

    init {
        initCheckBoxIsEnabled()
        initAppProperty()

        apply {
            add(checkBoxIsEnabled)
            add(cbDeploymentSlots, "growx")
        }
    }

    private fun initAppProperty() {
        appProperty.advise(lifetime) {
            cbDeploymentSlots.refreshItems()
        }
    }

    private fun initCheckBoxIsEnabled() {
        // Disable checkbox when no deployment slots are present
        checkBoxIsEnabled.addPropertyChangeListener("enabled") { event ->
            if (event?.newValue == false)
                checkBoxIsEnabled.isSelected = false
        }

        // Enable Deployment Slots combobox when checkbox is set
        checkBoxIsEnabled.addItemListener { event ->
            cbDeploymentSlots.isEnabled = event.stateChange == ItemEvent.SELECTED
        }
    }
}
