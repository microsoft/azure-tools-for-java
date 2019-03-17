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

package com.microsoft.intellij.component.database

import com.intellij.ui.HideableTitledPanel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.AzureResourceGroupSelector
import com.microsoft.intellij.component.extension.initValidationWithResult
import com.microsoft.intellij.component.extension.setComponentsEnabled
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel

/**
 * Form for creating new SQL Database
 *
 * Note: this component miss subscription, because we suppose to use in conjunction with Web App and
 * Function App publishing. Subscription is selected for a form entirely.
 */
class DatabaseCreateNewComponent(private val lifetime: Lifetime) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_SQL_SERVER = "SQL Server"
    }

    val pnlNewDatabaseName = DatabaseNameComponent(lifetime.createNested())

    val pnlResourceGroup = AzureResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(HEADER_RESOURCE_GROUP, pnlResourceGroup, true)

    val pnlSqlServer = SqlServerSelector(lifetime.createNested())
    private val pnlSqlServerHolder = HideableTitledPanel(HEADER_SQL_SERVER, pnlSqlServer, true)

    val pnlCollation = DatabaseCollationComponent()

    init {
        add(pnlNewDatabaseName, "growx")
        add(pnlResourceGroupHolder, "growx")
        add(pnlSqlServerHolder, "growx")
        add(pnlCollation, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    override fun initComponentValidation() {
        pnlNewDatabaseName.txtNameValue.initValidationWithResult(
                lifetime.createNested(),
                textChangeValidationAction = { SqlDatabaseValidator.checkInvalidCharacters(pnlNewDatabaseName.txtNameValue.text) },
                focusLostValidationAction = { ValidationResult() })

        pnlResourceGroup.initComponentValidation()
        pnlSqlServer.initComponentValidation()
    }

    //region Button Group

    private fun initButtonGroupsState() {
        initResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    private fun initResourceGroupButtonGroup() {

        // Remove all action listeners because we need to extra control over SQL Server button group here
        pnlResourceGroup.rdoCreateNew.actionListeners.forEach { pnlResourceGroup.rdoCreateNew.removeActionListener(it) }
        pnlResourceGroup.rdoUseExisting.actionListeners.forEach { pnlResourceGroup.rdoUseExisting.removeActionListener(it) }

        pnlResourceGroup.rdoCreateNew.addActionListener {
            pnlResourceGroup.toggleResourceGroupPanel(true)
            pnlSqlServer.rdoCreateSqlServer.doClick()
        }

        pnlResourceGroup.rdoUseExisting.addActionListener {
            // This logic does not allow to re-enable ResourceGroup ComboBox in case Existing Sql Server is selcted
            // since we should rely on resource group associated with selected SQL Server.
            pnlResourceGroup.toggleResourceGroupPanel(false)
            if (pnlSqlServer.rdoExistingSqlServer.isSelected)
                setComponentsEnabled(false, pnlResourceGroup.cbResourceGroup)
        }

        pnlResourceGroup.toggleResourceGroupPanel(false)
    }

    /**
     * Button groups - SQL Server
     *
     * Note: Existing SQL Server is already defined in some Resource Group. User has no option to choose
     *       a Resource Group if he selecting from existing SQL Servers.
     */
    private fun initSqlServerButtonsGroup() {
        pnlSqlServer.rdoCreateSqlServer.addActionListener {
            pnlResourceGroup.toggleResourceGroupPanel(pnlResourceGroup.rdoCreateNew.isSelected)
        }

        pnlSqlServer.rdoExistingSqlServer.addActionListener {
            val sqlServer = pnlSqlServer.lastSelectedSqlServer
            if (sqlServer != null) toggleSqlServerComboBox(sqlServer)

            pnlResourceGroup.rdoUseExisting.doClick()
            // Disable ability to select resource group - show related to SQL Server instead
            pnlResourceGroup.cbResourceGroup.isEnabled = false
        }

        pnlSqlServer.toggleSqlServerPanel(false)
    }

    private fun toggleSqlServerComboBox(selectedSqlServer: SqlServer) {
        val resourceGroupToSet =
                pnlResourceGroup.cachedResourceGroup.find { it.name() == selectedSqlServer.resourceGroupName() } ?: return

        pnlResourceGroup.cbResourceGroup.selectedItem = resourceGroupToSet
    }

    //endregion Button Group

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>, defaultResourceGroupName: String? = null) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == defaultResourceGroupName
        }
    }

    fun fillLocation(locations: List<Location>, defaultLocation: Region? = null) {
        pnlSqlServer.fillLocationComboBox(locations, defaultLocation)
    }

    fun fillSqlServer(sqlServers: List<SqlServer>, defaultSqlServerName: String? = null) {
        pnlSqlServer.fillSqlServerComboBox(sqlServers) { sqlServer -> sqlServer.name() == defaultSqlServerName }
    }
}
