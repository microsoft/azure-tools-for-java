/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.intellij.ui.extension.fillComboBox
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import com.microsoft.intellij.helpers.validator.StorageAccountValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class StorageAccountSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)
    }

    val rdoUseExisting = JRadioButton(message("run_config.publish.form.storage_account.use_existing"), true)
    val cbStorageAccount = ComboBox<StorageAccount>()

    val rdoCreateNew = JRadioButton(message("run_config.publish.form.storage_account.create_new"))
    val txtName = JTextField("")
    private val lblStorageAccountType = JLabel(message("run_config.publish.form.storage_account.type.label"))
    val cbStorageAccountType = ComboBox<StorageAccountSkuType>()

    var lastSelectedStorageAccount: StorageAccount? = null

    val isCreatingNew: Boolean
        get() = rdoCreateNew.isSelected

    val storageAccountName: String
        get() = txtName.text

    val storageAccountType: StorageAccountSkuType?
        get() = cbStorageAccountType.getSelectedValue()

    init {
        initStorageAccountComboBox()
        initStorageAccountTypeComboBox()
        initStorageAccountButtonsGroup()

        add(rdoUseExisting)
        add(cbStorageAccount, "growx")

        add(rdoCreateNew)
        add(txtName, "growx")
        add(lblStorageAccountType, "gapbefore $indentionSize")
        add(cbStorageAccountType, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()

        if (rdoUseExisting.isSelected) {
            return listOfNotNull(
                    StorageAccountValidator.checkStorageAccountIsSet(cbStorageAccount.getSelectedValue())
                            .toValidationInfo(cbStorageAccount))
        }

        return listOfNotNull(
                StorageAccountValidator.validateStorageAccountName(txtName.text).toValidationInfo(txtName))
    }

    override fun initComponentValidation() {
        txtName.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    StorageAccountValidator.checkStorageAccountNameMaxLength(txtName.text)
                            .merge(StorageAccountValidator.checkInvalidCharacters(txtName.text)) },
                focusLostValidationAction = { if (!isEnabled || rdoUseExisting.isSelected) return@initValidationWithResult ValidationResult()
                    if (txtName.text.isEmpty()) return@initValidationWithResult ValidationResult()
                    StorageAccountValidator.checkStorageAccountNameMinLength(txtName.text) })
    }

    fun fillStorageAccount(storageAccount: List<StorageAccount>, defaultStorageAccountId: String? = null) {
        cbStorageAccount.fillComboBox(
                elements = storageAccount,
                defaultComparator = { storage -> storage.id() == defaultStorageAccountId })
    }

    fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>, defaultStorageAccountType: StorageAccountSkuType? = null) {
        cbStorageAccountType.fillComboBox(storageAccountType, defaultStorageAccountType)
    }

    fun toggleStorageAccountPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew, txtName, lblStorageAccountType, cbStorageAccountType)
        setComponentsEnabled(!isCreatingNew, cbStorageAccount)
    }

    private fun initStorageAccountComboBox() {
        cbStorageAccount.addActionListener {
            lastSelectedStorageAccount = cbStorageAccount.getSelectedValue()
        }

        cbStorageAccount.setDefaultRenderer(message("run_config.publish.form.storage_account.empty_message")) { storageAccount ->
            "${storageAccount.name()} (${storageAccount.region().name()})"
        }
    }

    private fun initStorageAccountTypeComboBox() {
        cbStorageAccountType.setDefaultRenderer(message("run_config.publish.form.storage_account.type.empty_message")) { it.name().toString() }
    }

    private fun initStorageAccountButtonsGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { toggleStorageAccountPanel(false) },
                rdoCreateNew to ActionListener { toggleStorageAccountPanel(true) }))

        toggleStorageAccountPanel(false)
    }
}