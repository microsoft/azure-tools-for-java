/**
 * Copyright (c) 2019 JetBrains s.r.o.
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
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.intellij.component.extension.*
import com.microsoft.intellij.helpers.validator.StorageAccountValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class StorageAccountSelector(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")),
        AzureComponent {

    companion object {
        private const val EMPTY_STORAGE_ACCOUNT_MESSAGE = "No existing Storage Accounts"
        private const val EMPTY_STORAGE_ACCOUNT_TYPE_MESSAGE = "No existing Storage Account Types"

        private val indentionSize = JBUI.scale(17)
    }

    var subscriptionId: String = ""

    val rdoUseExisting = JRadioButton("Use Existing", true)
    val cbStorageAccount = ComboBox<StorageAccount>()

    val rdoCreateNew = JRadioButton("Create New")
    val txtName = JTextField("")
    private val lblStorageAccountType = JLabel("Type")
    val cbStorageAccountType = ComboBox<StorageAccountSkuType>()

    val isCreatingNew
        get() = rdoCreateNew.isSelected

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
                StorageAccountValidator.validateStorageAccountName(subscriptionId, txtName.text)
                        .toValidationInfo(txtName))
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
        cbStorageAccount.setDefaultRenderer(EMPTY_STORAGE_ACCOUNT_MESSAGE) {
            "${it.name()} (${it.region().name()})"
        }
    }

    private fun initStorageAccountTypeComboBox() {
        cbStorageAccountType.setDefaultRenderer(EMPTY_STORAGE_ACCOUNT_TYPE_MESSAGE) { it.name().name }
    }

    private fun initStorageAccountButtonsGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExisting to ActionListener { toggleStorageAccountPanel(false) },
                rdoCreateNew to ActionListener { toggleStorageAccountPanel(true) }))
    }
}