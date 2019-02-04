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

package com.microsoft.intellij.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.intellij.component.extension.*
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class AzureResourceGroupSelector(private val lifetimeDef: LifetimeDefinition) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    companion object {
        private const val EMPTY_RESOURCE_GROUP_MESSAGE = "No existing Azure Resource Groups"
    }

    val rdoExistingResourceGroup = JRadioButton("Use Existing", true)
    val cbResourceGroup = ComboBox<ResourceGroup>()

    val rdoCreateResourceGroup = JRadioButton("Create New")
    val txtResourceGroupName = JTextField("")

    var lastSelectedResourceGroup: ResourceGroup? = null
    var listenerAction: () -> Unit = {}

    var subscriptionId: String = ""

    init {
        initResourceGroupComboBox()
        initResourceGroupButtonGroup()

        add(rdoExistingResourceGroup)
        add(cbResourceGroup, "growx")

        add(rdoCreateResourceGroup)
        add(txtResourceGroupName, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (rdoExistingResourceGroup.isSelected)
            return listOfNotNull(ResourceGroupValidator.checkResourceGroupIsSet(cbResourceGroup.getSelectedValue())
                    .toValidationInfo(cbResourceGroup))

        return listOfNotNull(ResourceGroupValidator.validateResourceGroupName(txtResourceGroupName.text)
                .merge(ResourceGroupValidator.checkResourceGroupExistence(subscriptionId, txtResourceGroupName.text))
                .toValidationInfo(txtResourceGroupName))
    }

    override fun initComponentValidation() {
        txtResourceGroupName.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { if (rdoExistingResourceGroup.isSelected) return@initValidationWithResult ValidationResult()
                    ResourceGroupValidator.checkNameMaxLength(txtResourceGroupName.text)
                            .merge(ResourceGroupValidator.checkInvalidCharacters(txtResourceGroupName.text)) },
                focusLostValidationAction = { if (rdoExistingResourceGroup.isSelected) return@initValidationWithResult ValidationResult()
                    ResourceGroupValidator.checkEndsWithPeriod(txtResourceGroupName.text) })
    }

    private fun initResourceGroupComboBox() {
        cbResourceGroup.renderer = cbResourceGroup.createDefaultRenderer(EMPTY_RESOURCE_GROUP_MESSAGE) { it.name() }

        cbResourceGroup.addActionListener {
            val resourceGroup = cbResourceGroup.getSelectedValue() ?: return@addActionListener
            if (resourceGroup == lastSelectedResourceGroup) return@addActionListener

            listenerAction()
            lastSelectedResourceGroup = resourceGroup
        }
    }

    private fun initResourceGroupButtonGroup() {
        val resourceGroupButtons = ButtonGroup()

        resourceGroupButtons.add(rdoExistingResourceGroup)
        resourceGroupButtons.add(rdoCreateResourceGroup)

        rdoExistingResourceGroup.addActionListener { toggleResourceGroupPanel(false) }
        rdoCreateResourceGroup.addActionListener { toggleResourceGroupPanel(true) }

        toggleResourceGroupPanel(false)
    }

    private fun toggleResourceGroupPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtResourceGroupName)
        setComponentsEnabled(!isCreatingNew, cbResourceGroup)
    }
}
