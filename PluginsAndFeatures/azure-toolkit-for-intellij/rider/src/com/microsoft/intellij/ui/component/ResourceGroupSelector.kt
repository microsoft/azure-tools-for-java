/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class ResourceGroupSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    val rdoUseExisting = JRadioButton(message("run_config.publish.form.resource_group.use_existing"), true)
    val cbResourceGroup = ComboBox<ResourceGroup>()

    val rdoCreateNew = JRadioButton(message("run_config.publish.form.resource_group.create_new"))
    val txtResourceGroupName = JTextField("")

    var lastSelectedResourceGroup: ResourceGroup? = null
    var listenerAction: () -> Unit = {}

    var cachedResourceGroup: List<ResourceGroup> = emptyList()

    val isCreateNew: Boolean
        get() = rdoCreateNew.isSelected

    val resourceGroupName: String
        get() = txtResourceGroupName.text

    init {
        initResourceGroupComboBox()
        initResourceGroupButtonGroup()

        add(rdoUseExisting)
        add(cbResourceGroup, "growx")

        add(rdoCreateNew)
        add(txtResourceGroupName, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()
        if (rdoUseExisting.isSelected)
            return listOfNotNull(ResourceGroupValidator.checkResourceGroupIsSet(cbResourceGroup.getSelectedValue())
                    .toValidationInfo(cbResourceGroup))

        return listOfNotNull(ResourceGroupValidator.validateResourceGroupName(resourceGroupName)
                .toValidationInfo(txtResourceGroupName))
    }

    override fun initComponentValidation() {
        txtResourceGroupName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = {
                    if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    ResourceGroupValidator.checkResourceGroupNameMaxLength(resourceGroupName)
                            .merge(ResourceGroupValidator.checkInvalidCharacters(resourceGroupName)) },
                focusLostValidationAction = {
                    if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    ResourceGroupValidator.checkEndsWithPeriod(resourceGroupName) })
    }

    fun fillResourceGroupComboBox(resourceGroups: List<ResourceGroup>, defaultComparator: (ResourceGroup) -> Boolean = { false }) {
        cachedResourceGroup = resourceGroups
        cbResourceGroup.fillComboBox(resourceGroups.sortedBy { it.name() }, defaultComparator)

        if (resourceGroups.isEmpty()) {
            rdoCreateNew.doClick()
            lastSelectedResourceGroup = null
        }
    }

    fun toggleResourceGroupPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtResourceGroupName)
        setComponentsEnabled(!isCreatingNew, cbResourceGroup)
    }

    private fun initResourceGroupComboBox() {
        cbResourceGroup.setDefaultRenderer(message("run_config.publish.form.resource_group.empty_message")) { it.name() }

        cbResourceGroup.addActionListener {
            val resourceGroup = cbResourceGroup.getSelectedValue() ?: return@addActionListener
            if (resourceGroup == lastSelectedResourceGroup) return@addActionListener

            listenerAction()
            lastSelectedResourceGroup = resourceGroup
        }
    }

    private fun initResourceGroupButtonGroup() {
        val resourceGroupButtons = ButtonGroup()

        resourceGroupButtons.add(rdoUseExisting)
        resourceGroupButtons.add(rdoCreateNew)

        rdoUseExisting.addActionListener { toggleResourceGroupPanel(false) }
        rdoCreateNew.addActionListener { toggleResourceGroupPanel(true) }

        toggleResourceGroupPanel(false)
    }
}
