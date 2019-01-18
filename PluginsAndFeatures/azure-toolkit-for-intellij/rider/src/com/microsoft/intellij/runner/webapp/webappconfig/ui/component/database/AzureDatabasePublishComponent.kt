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

package com.microsoft.intellij.runner.webapp.webappconfig.ui.component.database

import com.intellij.ui.HideableTitledPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.component.*
import com.microsoft.intellij.component.extension.*
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.runner.webapp.model.DatabasePublishModel
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JRadioButton

class AzureDatabasePublishComponent(private val lifetime: Lifetime,
                                    private val model: DatabasePublishModel) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {

        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_SQL_SERVER = "SQL Server"

        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"
        private const val DEFAULT_SQL_DATABASE_NAME = "sql_%s_db"
        private const val DEFAULT_SQL_SERVER_NAME = "sql-server-"

        private val indentionSize = JBUI.scale(7)
    }

    private val isDbConnectionEnabled: Boolean
        get() = checkBoxEnableDbConnection.isEnabled

    private val isNewDatabase: Boolean
        get() = rdoCreateNewDb.isSelected

    // Subscription
    val pnlSubscription = AzureSubscriptionsSelector()

    // SQL Database
    private val checkBoxEnableDbConnection = JCheckBox("Enable database connection", true)

    // Db Selector
    private val pnlDbConnectionSelector = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2"))
    private val rdoExistingDb = JRadioButton("Use Existing Database", true)
    private val rdoCreateNewDb = JRadioButton("Create New Database")

    // Connection String
    private val pnlDbConnectionString = ConnectionStringComponent()

    // Existing Database
    private val pnlExistingDb = ExistingDatabaseComponent()

    // New Database
    private val pnlCreateNewDb = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    private val pnlNewDatabaseName = DatabaseNameComponent(lifetime.createNested())

    // Resource Group
    private val pnlResourceGroup = AzureResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(HEADER_RESOURCE_GROUP, pnlResourceGroup, true)

    // Database SQL Server
    private val pnlSqlServer = AzureSqlServerSelector(lifetime.createNested())
    private val pnlSqlServerHolder = HideableTitledPanel(HEADER_SQL_SERVER, pnlSqlServer, true)

    // Database Collation
    private val pnlCollation = DatabaseCollationComponent()

    init {
        initDbConnectionEnableCheckbox()
        initDatabaseConnectionSelectorPanel()
        initCreateNewDatabasePanel()

        add(checkBoxEnableDbConnection)
        add(pnlDbConnectionSelector)
        add(pnlExistingDb, "growx")
        add(pnlCreateNewDb, "growx")
        add(pnlDbConnectionString, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    fun resetFromConfig(config: DatabasePublishModel, dateString: String) {

        pnlExistingDb.cbDatabase.selectedItem = config.database

        pnlNewDatabaseName.txtNameValue.text =
                if (config.databaseName.isEmpty()) String.format(DEFAULT_SQL_DATABASE_NAME, dateString)
                else config.databaseName

        pnlResourceGroup.txtResourceGroupName.text = "$DEFAULT_RESOURCE_GROUP_NAME$dateString"
        pnlSqlServer.txtSqlServerName.text = "$DEFAULT_SQL_SERVER_NAME$dateString"

        pnlCollation.txtCollation.text = config.collation
        pnlDbConnectionString.txtConnectionStringName.text = config.connectionStringName

        if (config.isCreatingSqlDatabase) rdoCreateNewDb.doClick()
        else rdoExistingDb.doClick()

        if (config.isCreatingResourceGroup) pnlResourceGroup.rdoCreateResourceGroup.doClick()
        else pnlResourceGroup.rdoExistingResourceGroup.doClick()

        if (config.isCreatingSqlServer) pnlSqlServer.rdoCreateSqlServer.doClick()
        else pnlSqlServer.rdoExistingSqlServer.doClick()

        if (config.isDatabaseConnectionEnabled.xor(checkBoxEnableDbConnection.isSelected))
            checkBoxEnableDbConnection.doClick()
    }

    fun applyConfig(model: DatabasePublishModel) {
        model.subscription = pnlSubscription.cbSubscription.getSelectedValue()
        model.isDatabaseConnectionEnabled = checkBoxEnableDbConnection.isSelected

        model.connectionStringName = pnlDbConnectionString.txtConnectionStringName.text
        model.isCreatingSqlDatabase = isNewDatabase

        if (isNewDatabase) {
            model.databaseName = pnlNewDatabaseName.txtNameValue.text

            model.isCreatingResourceGroup = pnlResourceGroup.rdoCreateResourceGroup.isSelected
            if (pnlResourceGroup.rdoCreateResourceGroup.isSelected) {
                model.resourceGroupName = pnlResourceGroup.txtResourceGroupName.text
            } else {
                model.resourceGroupName = pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
            }

            model.isCreatingSqlServer = pnlSqlServer.rdoCreateSqlServer.isSelected
            if (pnlSqlServer.rdoCreateSqlServer.isSelected) {
                model.sqlServerName = pnlSqlServer.txtSqlServerName.text
                model.location = pnlSqlServer.lastSelectedLocation?.region() ?: model.location
                model.sqlServerAdminLogin = pnlSqlServer.txtAdminLogin.text
                model.sqlServerAdminPassword = pnlSqlServer.passNewAdminPasswordValue.password
                model.sqlServerAdminPasswordConfirm = pnlSqlServer.passNewAdminPasswordConfirmValue.password
            } else {
                model.sqlServerId = pnlSqlServer.cbSqlServer.getSelectedValue()?.id() ?: ""
                model.sqlServerAdminLogin = pnlSqlServer.lblAdminLoginValue.text
                model.sqlServerAdminPassword = pnlSqlServer.passExistingAdminPasswordValue.password
            }

            model.collation = pnlCollation.txtCollation.text
        } else {
            model.database = pnlExistingDb.cbDatabase.getSelectedValue()
            model.sqlServerAdminLogin = pnlExistingDb.lblExistingAdminLoginValue.text
            model.sqlServerAdminPassword = pnlExistingDb.passExistingDbAdminPassword.password
        }
    }

    //region Validation

    override fun initComponentValidation() {
        pnlNewDatabaseName.txtNameValue.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = {
                    if (!isDbConnectionEnabled || !isNewDatabase)
                        return@initValidationWithResult ValidationResult()

                    SqlDatabaseValidator.checkInvalidCharacters(pnlNewDatabaseName.txtNameValue.text)
                },
                focusLostValidationAction = { ValidationResult() })

        pnlResourceGroup.initComponentValidation()
        pnlSqlServer.initComponentValidation()
    }

    //endregion Validation

    //region Database Connection Panel

    private fun initDatabaseConnectionSelectorPanel() {
        pnlDbConnectionSelector.apply {
            add(rdoExistingDb)
            add(rdoCreateNewDb, "gapbefore $indentionSize")
        }
    }

    private fun initCreateNewDatabasePanel() {

        pnlCreateNewDb.apply {
            add(pnlNewDatabaseName, "growx")
            add(pnlResourceGroupHolder, "growx")
            add(pnlSqlServerHolder, "growx")
            add(pnlCollation, "growx")
        }
    }

    //endregion Database Connection Panel

    //region Button Group

    private fun initButtonGroupsState() {
        initDatabaseButtonGroup()
        initResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    private fun initDatabaseButtonGroup() {
        initButtonsGroup(hashMapOf(
                rdoCreateNewDb to ActionListener { toggleDatabasePanel(true) },
                rdoExistingDb to ActionListener { toggleDatabasePanel(false) } ))
        toggleDatabasePanel(model.isCreatingSqlDatabase)
    }

    private fun toggleDatabasePanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateNewDb)
        setComponentsVisible(!isCreatingNew, pnlExistingDb)
    }

    private fun initResourceGroupButtonGroup() {

        // Remove all action listeners because we need to extra control over SQL Server button group here
        pnlResourceGroup.rdoCreateResourceGroup.actionListeners.forEach { pnlResourceGroup.rdoCreateResourceGroup.removeActionListener(it) }
        pnlResourceGroup.rdoExistingResourceGroup.actionListeners.forEach { pnlResourceGroup.rdoExistingResourceGroup.removeActionListener(it) }

        pnlResourceGroup.rdoCreateResourceGroup.addActionListener {
            pnlResourceGroup.toggleResourceGroupPanel(true)
            pnlSqlServer.rdoCreateSqlServer.doClick()
        }

        pnlResourceGroup.rdoExistingResourceGroup.addActionListener {
            // This logic does not allow to re-enable ResourceGroup ComboBox in case Existing Sql Server is selcted
            // since we should rely on resource group associated with selected SQL Server.
            pnlResourceGroup.toggleResourceGroupPanel(false)
            if (pnlSqlServer.rdoExistingSqlServer.isSelected)
                setComponentsEnabled(false, pnlResourceGroup.cbResourceGroup)
        }

        pnlResourceGroup.toggleResourceGroupPanel(model.isCreatingResourceGroup)
    }

    /**
     * Button groups - SQL Server
     *
     * Note: Existing SQL Server is already defined in some Resource Group. User has no option to choose
     *       a Resource Group if he selecting from existing SQL Servers.
     */
    private fun initSqlServerButtonsGroup() {

        pnlSqlServer.rdoCreateSqlServer.addActionListener {
            pnlResourceGroup.toggleResourceGroupPanel(pnlResourceGroup.rdoCreateResourceGroup.isSelected)
        }

        pnlSqlServer.rdoExistingSqlServer.addActionListener {
            val sqlServer = pnlSqlServer.lastSelectedSqlServer
            if (sqlServer != null) toggleSqlServerComboBox(sqlServer)

            pnlResourceGroup.rdoExistingResourceGroup.doClick()
            // Disable ability to select resource group - show related to SQL Server instead
            pnlResourceGroup.cbResourceGroup.isEnabled = false
        }

        pnlSqlServer.toggleSqlServerPanel(model.isCreatingSqlServer)
    }

    private fun toggleSqlServerComboBox(selectedSqlServer: SqlServer) {
        val resourceGroupToSet =
                pnlResourceGroup.cachedResourceGroup.find { it.name() == selectedSqlServer.resourceGroupName() } ?: return

        pnlResourceGroup.cbResourceGroup.selectedItem = resourceGroupToSet
    }

    private fun initDbConnectionEnableCheckbox() {
        checkBoxEnableDbConnection
                .addActionListener { enableDbConnectionPanel(checkBoxEnableDbConnection.isSelected) }
    }

    private fun enableDbConnectionPanel(isEnabled: Boolean) {
        setComponentsEnabled(isEnabled, rdoExistingDb, rdoCreateNewDb)
        pnlDbConnectionString.setComponentEnabled(isEnabled)

        pnlExistingDb.setComponentEnabled(isEnabled)
        pnlCreateNewDb.setComponentEnabled(isEnabled)
        if (pnlSqlServer.rdoExistingSqlServer.isSelected) {
            setComponentsEnabled(false, pnlResourceGroup.cbResourceGroup)
        }
    }

    //endregion Button Group

    fun fillSubscription(subscriptions: List<Subscription>) {
        pnlSubscription.fillSubscriptionComboBox(subscriptions, model.subscription)
    }

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == model.resourceGroupName
        }
    }

    fun fillLocation(locations: List<Location>) {
        pnlSqlServer.fillLocationComboBox(locations, model.location)
    }

    fun fillSqlDatabase(sqlDatabases: List<SqlDatabase>) {
        pnlExistingDb.fillSqlDatabase(sqlDatabases) { sqlDatabase -> sqlDatabase.databaseId() == model.database?.databaseId() }
    }

    fun fillSqlServer(sqlServers: List<SqlServer>) {
        pnlSqlServer.fillSqlServerComboBox(sqlServers) { sqlServer -> sqlServer.name() == model.sqlServerName }
    }
}
