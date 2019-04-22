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

package com.microsoft.intellij.runner.database.config.ui

import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ExistingOrNewSelector
import com.microsoft.intellij.ui.component.database.ConnectionStringComponent
import com.microsoft.intellij.ui.component.database.DatabaseCreateNewComponent
import com.microsoft.intellij.ui.component.database.ExistingDatabaseComponent
import com.microsoft.intellij.ui.extension.*
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import net.miginfocom.swing.MigLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

class DatabasePublishComponent(private val lifetime: Lifetime,
                               private val model: DatabasePublishModel,
                               var subscription: Subscription? = null) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"
        private const val DEFAULT_SQL_DATABASE_NAME = "sql_%s_db"
        private const val DEFAULT_SQL_SERVER_NAME = "sql-server-"
    }

    private val isDbConnectionEnabled: Boolean
        get() = checkBoxEnableDbConnection.isEnabled

    private val checkBoxEnableDbConnection = JCheckBox("Enable database connection", true)
    private val pnlDbSelector = ExistingOrNewSelector("Database")
    private val pnlDbConnectionString = ConnectionStringComponent()
    private val pnlExistingDb = ExistingDatabaseComponent()
    private val pnlCreateNewDb = DatabaseCreateNewComponent(lifetime.createNested())

    init {
        initDbConnectionEnableCheckbox()

        add(checkBoxEnableDbConnection)
        add(pnlDbSelector)
        add(pnlExistingDb, "growx")
        add(pnlCreateNewDb, "growx")
        add(pnlDbConnectionString, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    fun resetFromConfig(config: DatabasePublishModel, dateString: String) {

        pnlCreateNewDb.pnlNewDatabaseName.txtNameValue.text =
                if (config.databaseName.isEmpty()) String.format(DEFAULT_SQL_DATABASE_NAME, dateString)
                else config.databaseName

        pnlCreateNewDb.pnlResourceGroup.txtResourceGroupName.text = "$DEFAULT_RESOURCE_GROUP_NAME$dateString"
        pnlCreateNewDb.pnlSqlServer.txtSqlServerName.text = "$DEFAULT_SQL_SERVER_NAME$dateString"

        pnlCreateNewDb.pnlCollation.txtCollation.text = config.collation
        pnlDbConnectionString.txtConnectionStringName.text = config.connectionStringName

        if (config.isCreatingSqlDatabase) pnlDbSelector.rdoCreateNew.doClick()
        else pnlDbSelector.rdoUseExisting.doClick()

        if (config.isCreatingResourceGroup) pnlCreateNewDb.pnlResourceGroup.rdoCreateNew.doClick()
        else pnlCreateNewDb.pnlResourceGroup.rdoUseExisting.doClick()

        if (config.isCreatingSqlServer) pnlCreateNewDb.pnlSqlServer.rdoCreateSqlServer.doClick()
        else pnlCreateNewDb.pnlSqlServer.rdoExistingSqlServer.doClick()

        if (config.isDatabaseConnectionEnabled.xor(checkBoxEnableDbConnection.isSelected))
            checkBoxEnableDbConnection.doClick()
    }

    fun applyConfig(model: DatabasePublishModel) {
        model.subscription = subscription
        model.isDatabaseConnectionEnabled = checkBoxEnableDbConnection.isSelected

        model.connectionStringName = pnlDbConnectionString.txtConnectionStringName.text
        model.isCreatingSqlDatabase = pnlDbSelector.isCreateNew

        if (pnlDbSelector.isCreateNew) {
            model.databaseName = pnlCreateNewDb.pnlNewDatabaseName.txtNameValue.text

            model.isCreatingResourceGroup = pnlCreateNewDb.pnlResourceGroup.rdoCreateNew.isSelected
            if (pnlCreateNewDb.pnlResourceGroup.rdoCreateNew.isSelected) {
                model.resourceGroupName = pnlCreateNewDb.pnlResourceGroup.txtResourceGroupName.text
            } else {
                model.resourceGroupName = pnlCreateNewDb.pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
            }

            model.isCreatingSqlServer = pnlCreateNewDb.pnlSqlServer.rdoCreateSqlServer.isSelected
            if (pnlCreateNewDb.pnlSqlServer.rdoCreateSqlServer.isSelected) {
                model.sqlServerName = pnlCreateNewDb.pnlSqlServer.txtSqlServerName.text
                model.location = pnlCreateNewDb.pnlSqlServer.lastSelectedLocation?.region() ?: model.location
                model.sqlServerAdminLogin = pnlCreateNewDb.pnlSqlServer.txtAdminLogin.text
                model.sqlServerAdminPassword = pnlCreateNewDb.pnlSqlServer.passNewAdminPasswordValue.password
                model.sqlServerAdminPasswordConfirm = pnlCreateNewDb.pnlSqlServer.passNewAdminPasswordConfirmValue.password
            } else {
                model.sqlServerId = pnlCreateNewDb.pnlSqlServer.cbSqlServer.getSelectedValue()?.id() ?: ""
                model.sqlServerAdminLogin = pnlCreateNewDb.pnlSqlServer.lblAdminLoginValue.text
                model.sqlServerAdminPassword = pnlCreateNewDb.pnlSqlServer.passExistingAdminPasswordValue.password
            }

            model.collation = pnlCreateNewDb.pnlCollation.txtCollation.text
        } else {
            model.databaseId = pnlExistingDb.cbDatabase.getSelectedValue()?.id() ?: ""
            model.sqlServerAdminLogin = pnlExistingDb.lblExistingAdminLoginValue.text
            model.sqlServerAdminPassword = pnlExistingDb.passExistingDbAdminPassword.password
        }
    }

    //region Validation

    override fun initComponentValidation() {
        pnlCreateNewDb.pnlNewDatabaseName.txtNameValue.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = {
                    if (!isDbConnectionEnabled || !pnlDbSelector.isCreateNew)
                        return@initValidationWithResult ValidationResult()

                    SqlDatabaseValidator.checkInvalidCharacters(pnlCreateNewDb.pnlNewDatabaseName.txtNameValue.text)
                },
                focusLostValidationAction = { ValidationResult() })

        pnlCreateNewDb.pnlResourceGroup.initComponentValidation()
        pnlCreateNewDb.pnlSqlServer.initComponentValidation()
    }

    //endregion Validation

    //region Button Group

    private fun initDbConnectionEnableCheckbox() {
        checkBoxEnableDbConnection
                .addActionListener { enableDbConnectionPanel(checkBoxEnableDbConnection.isSelected) }
    }

    private fun enableDbConnectionPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled, pnlDbSelector.rdoUseExisting, pnlDbSelector.rdoCreateNew)
        pnlDbConnectionString.setComponentEnabled(isEnabled)

        pnlExistingDb.setComponentEnabled(isEnabled)
        pnlCreateNewDb.setComponentEnabled(isEnabled)
        if (pnlCreateNewDb.pnlSqlServer.rdoExistingSqlServer.isSelected) {
            setComponentsEnabled(false, pnlCreateNewDb.pnlResourceGroup.cbResourceGroup)
        }
    }

    private fun initButtonGroupsState() {
        initDatabaseButtonGroup()
        initResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    private fun initDatabaseButtonGroup() {
        pnlDbSelector.rdoCreateNew.addActionListener { toggleDatabasePanel(true) }
        pnlDbSelector.rdoUseExisting.addActionListener { toggleDatabasePanel(false) }
        toggleDatabasePanel(model.isCreatingSqlDatabase)
    }

    private fun toggleDatabasePanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateNewDb)
        setComponentsVisible(!isCreatingNew, pnlExistingDb)
    }

    private fun initResourceGroupButtonGroup() {
        pnlCreateNewDb.pnlResourceGroup.toggleResourceGroupPanel(model.isCreatingResourceGroup)
    }

    private fun initSqlServerButtonsGroup() {
        pnlCreateNewDb.pnlSqlServer.toggleSqlServerPanel(model.isCreatingSqlServer)
    }

    //endregion Button Group

    fun fillSqlDatabase(sqlDatabases: List<SqlDatabase>) =
            pnlExistingDb.fillSqlDatabase(sqlDatabases, model.databaseId)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) =
            pnlCreateNewDb.fillResourceGroup(resourceGroups, model.resourceGroupName)

    fun fillLocation(locations: List<Location>) =
            pnlCreateNewDb.fillLocation(locations, model.location)

    fun fillSqlServer(sqlServers: List<SqlServer>) =
            pnlCreateNewDb.fillSqlServer(sqlServers, model.sqlServerName)
}
