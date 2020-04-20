/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.ui.component.database

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.extension.*
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.*

class SqlServerSelector(private val lifetimeDef: LifetimeDefinition) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]", "[sg a]")),
        AzureComponent {

    companion object {
        private val indentionSize = JBUI.scale(17)
    }

    val rdoExistingSqlServer = JRadioButton(message("run_config.publish.form.sql_server.use_existing"), true)
    val cbSqlServer = ComboBox<SqlServer>()
    private val lblExistingLocationName = JLabel(message("run_config.publish.form.sql_server.location.label"))
    private val lblLocationValue = JLabel(message("common.na_capitalized"))
    private val lblExistingAdminLoginName = JLabel(message("run_config.publish.form.sql_server.admin_login.label"))
    val lblAdminLoginValue = JLabel(message("common.na_capitalized"))
    private val lblExistingAdminPasswordName = JLabel(message("run_config.publish.form.sql_server.admin_password.label"))
    val passExistingAdminPasswordValue = JBPasswordField()

    val rdoCreateSqlServer = JRadioButton(message("run_config.publish.form.sql_server.create_new"))
    private val pnlSqlServerName = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    val txtSqlServerName = JTextField("")
    private val lblSqlServerNameSuffix = JLabel(".database.windows.net")
    private val lblCreateLocationName = JLabel(message("run_config.publish.form.sql_server.location.label"))
    private val cbLocation = ComboBox<Location>()
    private val lblCreateAdminLoginName = JLabel(message("run_config.publish.form.sql_server.admin_login.label"))
    val txtAdminLogin = JTextField("")
    private val lblCreateAdminPasswordName = JLabel(message("run_config.publish.form.sql_server.admin_password.label"))
    val passNewAdminPasswordValue = JBPasswordField()
    private val lblCreateAdminPasswordConfirmName = JLabel(message("run_config.publish.form.sql_server.confirm_password.label"))
    val passNewAdminPasswordConfirmValue = JBPasswordField()

    var lastSelectedSqlServer: SqlServer? = null
    var lastSelectedLocation: Location? = null
    var listenerAction: () -> Unit = {}

    var subscriptionId: String = ""

    init {
        initSqlServerComboBox()
        initLocationComboBox()
        initSqlServerButtonsGroup()

        add(rdoExistingSqlServer)
        add(cbSqlServer, "growx")
        add(lblExistingLocationName, "gapbefore $indentionSize")
        add(lblLocationValue, "growx")
        add(lblExistingAdminLoginName, "gapbefore $indentionSize")
        add(lblAdminLoginValue, "growx")
        add(lblExistingAdminPasswordName, "gapbefore $indentionSize")
        add(passExistingAdminPasswordValue, "growx")

        add(rdoCreateSqlServer)
        pnlSqlServerName.add(txtSqlServerName, "growx")
        pnlSqlServerName.add(lblSqlServerNameSuffix)
        add(pnlSqlServerName, "growx")
        add(lblCreateLocationName, "gapbefore $indentionSize")
        add(cbLocation, "growx")
        add(lblCreateAdminLoginName, "gapbefore $indentionSize")
        add(txtAdminLogin, "growx")
        add(lblCreateAdminPasswordName, "gapbefore $indentionSize")
        add(passNewAdminPasswordValue, "growx")
        add(lblCreateAdminPasswordConfirmName, "gapbefore $indentionSize")
        add(passNewAdminPasswordConfirmValue, "growx")

        initComponentValidation()
    }

    override fun validateComponent(): List<ValidationInfo> {
        if (!isEnabled) return emptyList()

        if (rdoExistingSqlServer.isSelected) {
            return listOfNotNull(
                    SqlServerValidator.checkSqlServerIsSet(cbSqlServer.getSelectedValue()).toValidationInfo(cbSqlServer),
                    SqlServerValidator.checkPasswordIsSet(passExistingAdminPasswordValue.password).toValidationInfo(passExistingAdminPasswordValue))
        }

        return listOfNotNull(
                SqlServerValidator.validateSqlServerName(txtSqlServerName.text)
                        .merge(SqlServerValidator.checkSqlServerExistence(subscriptionId, txtSqlServerName.text))
                        .toValidationInfo(txtSqlServerName),
                SqlServerValidator.validateAdminLogin(txtAdminLogin.text).toValidationInfo(txtAdminLogin),
                SqlServerValidator.validateAdminPassword(txtAdminLogin.text, passNewAdminPasswordValue.password).toValidationInfo(passNewAdminPasswordValue),
                SqlServerValidator.checkPasswordsMatch(passNewAdminPasswordValue.password, passNewAdminPasswordConfirmValue.password)
                        .toValidationInfo(passNewAdminPasswordConfirmValue))
    }

    override fun initComponentValidation() {
        txtSqlServerName.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkNameMaxLength(txtSqlServerName.text)
                            .merge(SqlServerValidator.checkInvalidCharacters(txtSqlServerName.text)) },
                focusLostValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkStartsEndsWithDash(txtSqlServerName.text) })

        txtAdminLogin.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkLoginInvalidCharacters(txtAdminLogin.text) },
                focusLostValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkRestrictedLogins(txtAdminLogin.text) })

        passNewAdminPasswordValue.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkPasswordContainsUsername(passNewAdminPasswordValue.password, txtAdminLogin.text) },
                focusLostValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkPasswordRequirements(passNewAdminPasswordValue.password).merge(
                            if (passNewAdminPasswordValue.password.isEmpty()) ValidationResult()
                            else SqlServerValidator.checkPasswordMinLength(passNewAdminPasswordValue.password)) })

        passNewAdminPasswordConfirmValue.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { ValidationResult() },
                focusLostValidationAction = { if (!isEnabled || rdoExistingSqlServer.isSelected) return@initValidationWithResult ValidationResult()
                    SqlServerValidator.checkPasswordsMatch(passNewAdminPasswordValue.password, passNewAdminPasswordConfirmValue.password) })
    }

    fun fillSqlServerComboBox(sqlServers: List<SqlServer>, defaultComparator: (SqlServer) -> Boolean = { false }) {
        cbSqlServer.fillComboBox(sqlServers.sortedWith(compareBy { it.name() }), defaultComparator)

        if (sqlServers.isEmpty()) {
            lastSelectedSqlServer = null
        }
    }

    fun fillLocationComboBox(locations: List<Location>, defaultLocation: Region? = AzureDefaults.location) {
        cbLocation.fillComboBox<Location>(locations) { location -> location.region() == defaultLocation }

        if (locations.isEmpty()) {
            lastSelectedLocation = null
        }
    }

    /**
     * Set SQL Server elements visibility when using existing or creating new SQL Server
     *
     * @param isCreatingNew - flag indicating whether we create new SQL Server or use and existing one
     */
    fun toggleSqlServerPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew,
                txtSqlServerName, txtAdminLogin, passNewAdminPasswordValue, passNewAdminPasswordConfirmValue, cbLocation)

        setComponentsEnabled(!isCreatingNew,
                cbSqlServer, lblLocationValue, lblAdminLoginValue, passExistingAdminPasswordValue)
    }

    private fun initSqlServerComboBox() {
        cbSqlServer.setDefaultRenderer(message("run_config.publish.form.sql_server.empty_message")) { it.name() }

        cbSqlServer.addActionListener {
            val sqlServer = cbSqlServer.getSelectedValue() ?: return@addActionListener
            if (sqlServer == lastSelectedSqlServer) return@addActionListener

            lblLocationValue.text = sqlServer.region().label()
            lblAdminLoginValue.text = sqlServer.administratorLogin()

            listenerAction()
            lastSelectedSqlServer = sqlServer
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(message("run_config.publish.form.sql_server.location.empty_message")) { it.displayName() }

        cbLocation.addActionListener {
            lastSelectedLocation = cbLocation.getSelectedValue() ?: return@addActionListener
        }
    }

    /**
     * Button groups - SQL Server
     *
     * Note: Existing SQL Server is already defined in some Resource Group. User has no option to choose
     *       a Resource Group if he selecting from existing SQL Servers.
     */
    private fun initSqlServerButtonsGroup() {
        val btnGroupSqlServer = ButtonGroup()
        btnGroupSqlServer.add(rdoCreateSqlServer)
        btnGroupSqlServer.add(rdoExistingSqlServer)

        rdoCreateSqlServer.addActionListener { toggleSqlServerPanel(true) }
        rdoExistingSqlServer.addActionListener { toggleSqlServerPanel(false) }

        toggleSqlServerPanel(false)
    }
}
