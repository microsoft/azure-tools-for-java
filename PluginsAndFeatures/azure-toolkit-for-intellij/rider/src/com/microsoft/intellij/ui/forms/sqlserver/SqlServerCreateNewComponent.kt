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

package com.microsoft.intellij.ui.forms.sqlserver

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ResourceGroupSelector
import com.microsoft.intellij.ui.component.AzureResourceNameComponent
import com.microsoft.intellij.ui.component.SubscriptionSelector
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class SqlServerCreateNewComponent(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")),
        AzureComponent {

    val pnlName = AzureResourceNameComponent()
    val pnlSubscription = SubscriptionSelector()
    val pnlResourceGroup = ResourceGroupSelector(lifetime.createNested())

    private val pnlSqlServerSettings = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]"))

    private val lblLocation = JLabel(RiderAzureBundle.message("dialog.create_sql_server.location.label"))
    val cbLocation = ComboBox<Location>()

    private val lblAdminLogin = JLabel(RiderAzureBundle.message("dialog.create_sql_server.admin_login.label"))
    val txtAdminLoginValue = JTextField()

    private val lblAdminPassword = JLabel(RiderAzureBundle.message("dialog.create_sql_server.admin_password.label"))
    val passAdminPassword = JPasswordField()

    private val lblAdminPasswordConfirm = JLabel(RiderAzureBundle.message("dialog.create_sql_server.confirm_password.label"))
    val passAdminPasswordConfirm = JPasswordField()

    private var cachedResourceGroups = emptyList<ResourceGroup>()

    val serverName: String
        get() = pnlName.txtNameValue.text

    val adminLogin: String
        get() = txtAdminLoginValue.text

    val adminPassword: CharArray
        get() = passAdminPassword.password

    init {
        pnlResourceGroup.apply {
            border = IdeaTitledBorder(RiderAzureBundle.message("dialog.create_sql_server.resource_group.header"), 0, JBUI.emptyInsets())
        }

        pnlSqlServerSettings.apply {
            border = IdeaTitledBorder(RiderAzureBundle.message("dialog.create_sql_server.server_settings.header"), 0, JBUI.emptyInsets())

            add(lblLocation, "gapbefore 3")
            add(cbLocation, "growx")

            add(lblAdminLogin, "gapbefore 3")
            add(txtAdminLoginValue, "growx")

            add(lblAdminPassword, "gapbefore 3")
            add(passAdminPassword, "growx")

            add(lblAdminPasswordConfirm, "gapbefore 3")
            add(passAdminPasswordConfirm, "growx")
        }

        add(pnlName, "growx")
        add(pnlSubscription, "growx")
        add(pnlResourceGroup, "growx")
        add(pnlSqlServerSettings, "growx")

        initLocationComboBox()

        initComponentValidation()
    }

    override fun validateComponent() =
            pnlSubscription.validateComponent() +
                    pnlResourceGroup.validateComponent() +
                    listOfNotNull(
                            SqlServerValidator
                                    .validateSqlServerName(pnlName.txtNameValue.text)
                                    .merge(SqlServerValidator.checkSqlServerExistence(pnlSubscription.lastSelectedSubscriptionId, pnlName.txtNameValue.text))
                                    .toValidationInfo(pnlName.txtNameValue),
                            LocationValidator.checkLocationIsSet(cbLocation.getSelectedValue()).toValidationInfo(cbLocation),
                            SqlServerValidator.validateAdminLogin(txtAdminLoginValue.text).toValidationInfo(txtAdminLoginValue),
                            SqlServerValidator.validateAdminPassword(txtAdminLoginValue.text, passAdminPassword.password).toValidationInfo(passAdminPassword),
                            SqlServerValidator.checkPasswordsMatch(passAdminPassword.password, passAdminPasswordConfirm.password).toValidationInfo(passAdminPasswordConfirm)
                    )

    override fun initComponentValidation() {

        pnlName.txtNameValue.initValidationWithResult(
                lifetime,
                textChangeValidationAction = { SqlServerValidator.checkSqlServerNameMaxLength(pnlName.txtNameValue.text)
                        .merge(SqlServerValidator.checkInvalidCharacters(pnlName.txtNameValue.text)) },
                focusLostValidationAction = { SqlServerValidator.checkStartsEndsWithDash(pnlName.txtNameValue.text) })

        txtAdminLoginValue.initValidationWithResult(
                lifetime,
                textChangeValidationAction = { SqlServerValidator.checkLoginInvalidCharacters(txtAdminLoginValue.text) },
                focusLostValidationAction = { SqlServerValidator.checkRestrictedLogins(txtAdminLoginValue.text) })

        passAdminPassword.initValidationWithResult(
                lifetime,
                textChangeValidationAction = { SqlServerValidator.checkPasswordContainsUsername(passAdminPassword.password, txtAdminLoginValue.text) },
                focusLostValidationAction = { SqlServerValidator.checkPasswordRequirements(passAdminPassword.password).merge(
                        if (passAdminPassword.password.isEmpty()) ValidationResult()
                        else SqlServerValidator.checkPasswordMinLength(passAdminPassword.password)) })

        passAdminPasswordConfirm.initValidationWithResult(
                lifetime,
                textChangeValidationAction = { ValidationResult() },
                focusLostValidationAction = { SqlServerValidator.checkPasswordsMatch(passAdminPassword.password, passAdminPasswordConfirm.password) })
    }

    fun fillSubscription(subscriptions: List<Subscription>) {
        pnlSubscription.cbSubscription.removeAllItems()

        subscriptions.sortedWith(compareBy { it.displayName() }).forEach { subscription ->
            pnlSubscription.cbSubscription.addItem(subscription)
        }

        if (subscriptions.isEmpty()) {
            pnlSubscription.lastSelectedSubscriptionId = ""
        }
        setComponentsEnabled(true, pnlSubscription.cbSubscription)
    }

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        cachedResourceGroups = resourceGroups
        pnlResourceGroup.cbResourceGroup.removeAllItems()

        resourceGroups.sortedWith(compareBy { it.name() }).forEach { resourceGroup ->
            pnlResourceGroup.cbResourceGroup.addItem(resourceGroup)
        }

        if (resourceGroups.isEmpty()) {
            pnlResourceGroup.rdoCreateNew.doClick()
            pnlResourceGroup.lastSelectedResourceGroup = null
        }
        setComponentsEnabled(true, pnlResourceGroup.cbResourceGroup, pnlResourceGroup.rdoUseExisting)
    }

    fun fillLocation(locations: List<Location>) {
        cbLocation.removeAllItems()

        locations.sortedWith(compareBy { it.displayName() }).forEach { location ->
            cbLocation.addItem(location)
            if (location.region() == AzureDefaults.location)
                cbLocation.selectedItem = location
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(RiderAzureBundle.message("dialog.create_sql_server.location.empty_message")) { it.displayName() }
    }
}
