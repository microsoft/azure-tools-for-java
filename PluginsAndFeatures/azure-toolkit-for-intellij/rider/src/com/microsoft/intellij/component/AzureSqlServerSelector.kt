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
import com.intellij.ui.components.JBPasswordField
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.component.extension.createDefaultRenderer
import com.microsoft.intellij.component.extension.getSelectedValue
import com.microsoft.intellij.component.extension.setComponentsEnabled
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import net.miginfocom.swing.MigLayout
import javax.swing.*

class AzureSqlServerSelector : JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]")), AzureComponent {

    companion object {
        private const val EMPTY_SQL_SERVER_MESSAGE = "No existing SQL Servers"
    }

    private val rdoExistingSqlServer = JRadioButton("Use Existing", true)
    private val cbSqlServer = ComboBox<SqlServer>()
    private val lblExistingLocationName = JLabel("Location")
    private val lblLocationValue = JLabel("N/A")
    private val lblExistingAdminLoginName = JLabel("Admin Login")
    private val lblAdminLoginValue = JLabel("N/A")
    private val lblExistingAdminPasswordName = JLabel("Admin Password")
    private val passExistingAdminPasswordValue = JBPasswordField()

    private val rdoCreateSqlServer = JRadioButton("Create New")
    private val pnlSqlServerName = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    private val txtSqlServerName = JTextField("")
    private val lblSqlServerNameSuffix = JLabel(".database.windows.net")
    private val lblCreateLocationName = JLabel("Location")
    private val cbLocation = ComboBox<Location>()
    private val lblCreateAdminLoginName = JLabel("Admin Login")
    private val txtAdminLogin = JTextField("")
    private val lblCreateAdminPasswordName = JLabel("Admin Password")
    private val passNewAdminPasswordValue = JBPasswordField()
    private val lblCreateAdminPasswordConfirmName = JLabel("Confirm Password")
    private val passNewAdminPasswordConfirmValue = JBPasswordField()

    var lastSelectedSqlServer: SqlServer? = null
    var listenerAction: () -> Unit = {}

    init {
        initSqlServerComboBox()
        initSqlServerButtonsGroup()

        add(rdoExistingSqlServer)
        add(cbSqlServer, "growx")
        add(lblExistingLocationName)
        add(lblLocationValue, "growx")
        add(lblExistingAdminLoginName)
        add(lblAdminLoginValue, "growx")
        add(lblExistingAdminPasswordName)
        add(passExistingAdminPasswordValue, "growx")

        add(rdoCreateSqlServer)
        pnlSqlServerName.add(txtSqlServerName, "growx")
        pnlSqlServerName.add(lblSqlServerNameSuffix)
        add(pnlSqlServerName, "growx")
        add(lblCreateLocationName)
        add(cbLocation, "growx")
        add(lblCreateAdminLoginName)
        add(txtAdminLogin, "growx")
        add(lblCreateAdminPasswordName)
        add(passNewAdminPasswordValue, "growx")
        add(lblCreateAdminPasswordConfirmName)
        add(passNewAdminPasswordConfirmValue, "growx")
    }

    override fun validateComponent(): ValidationInfo? {
        var component: JComponent = txtSqlServerName

        val status =
                if (rdoCreateSqlServer.isSelected) {
                    SqlServerValidator.validateSqlServerName(txtSqlServerName.text)
                    SqlServerValidator.validateAdminLogin(txtAdminLogin.text)
                    SqlServerValidator.validateAdminPassword(txtAdminLogin.text, passNewAdminPasswordValue.password)
                    SqlServerValidator.checkPasswordsMatch(passNewAdminPasswordValue.password, passNewAdminPasswordConfirmValue.password)
                } else {
                    component = cbSqlServer
                    SqlServerValidator.checkSqlServerIsSet(cbSqlServer.getSelectedValue())
                    SqlServerValidator.checkPasswordIsSet(passExistingAdminPasswordValue.password)
                }

        if (!status.isValid)
            return ValidationInfo(status.errors.first(), component)

        return null
    }

    private fun initSqlServerComboBox() {
        cbSqlServer.renderer = cbSqlServer.createDefaultRenderer(EMPTY_SQL_SERVER_MESSAGE) { "${it.name()} (${it.resourceGroupName()})" }

        cbSqlServer.addActionListener {
            val sqlServer = cbSqlServer.getSelectedValue() ?: return@addActionListener
            if (sqlServer == lastSelectedSqlServer) return@addActionListener

            lblLocationValue.text = sqlServer.region().label()
            lblAdminLoginValue.text = sqlServer.administratorLogin()

            listenerAction()
            lastSelectedSqlServer = sqlServer
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

    /**
     * Set SQL Server elements visibility when using existing or creating new SQL Server
     *
     * @param isCreatingNew - flag indicating whether we create new SQL Server or use and existing one
     */
    private fun toggleSqlServerPanel(isCreatingNew: Boolean) {
        setComponentsEnabled(isCreatingNew,
                txtSqlServerName, txtAdminLogin, passNewAdminPasswordValue, passNewAdminPasswordConfirmValue, cbLocation)

        setComponentsEnabled(!isCreatingNew,
                cbSqlServer, lblLocationValue, lblAdminLoginValue, passExistingAdminPasswordValue)
    }
}
