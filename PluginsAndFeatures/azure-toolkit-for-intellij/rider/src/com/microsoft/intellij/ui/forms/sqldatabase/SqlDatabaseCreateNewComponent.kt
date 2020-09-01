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

package com.microsoft.intellij.ui.forms.sqldatabase

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.AzureResourceNameComponent
import com.microsoft.intellij.ui.component.SubscriptionSelector
import com.microsoft.intellij.ui.extension.*
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class SqlDatabaseCreateNewComponent(private val lifetime: Lifetime, private val sqlServer: SqlServer) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1")),
        AzureComponent {

    companion object {
        private const val AZURE_SQL_DATABASE_PRICING_URI = "https://azure.microsoft.com/en-us/pricing/details/sql-database/"
    }

    val pnlName = AzureResourceNameComponent()
    val pnlSubscription = SubscriptionSelector()

    val pnlResourceGroup = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    val cbResourceGroup = ComboBox<ResourceGroup>()

    private val pnlSqlDatabaseSettings = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]"))
    private val lblSqlServer = JLabel(RiderAzureBundle.message("dialog.create_sql_db.sql_server.label"))
    private val cbSqlServer = ComboBox<SqlServer>()

    private val lblDatabaseEdition = JLabel(RiderAzureBundle.message("dialog.create_sql_db.edition.label"))
    private val pnlDatabaseEdition = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    val cbDatabaseEdition = ComboBox<DatabaseEdition>()
    private val lblDatabasePricingLink =
            LinkLabel(RiderAzureBundle.message("dialog.create_sql_db.pricing.label"), null, { _, link -> BrowserUtil.browse(link)}, AZURE_SQL_DATABASE_PRICING_URI)

    private val lblDatabaseComputeSize = JLabel(RiderAzureBundle.message("dialog.create_sql_db.compute_size.label"))
    val cbDatabaseComputeSize = ComboBox<ServiceObjectiveName>()

    private val lblDatabaseCollation = JLabel(RiderAzureBundle.message("dialog.create_sql_db.collation.label"))
    val txtDatabaseCollation = JTextField(AzureDefaults.SQL_DATABASE_COLLATION)

    private var lastSelectedDatabaseEdition: DatabaseEdition = DatabaseEdition()
    private var cachedComputeSize = listOf<ServiceObjectiveName>()

    val databaseName: String
        get() = pnlName.txtNameValue.text

    val collation: String
        get() = txtDatabaseCollation.text

    init {
        pnlResourceGroup.apply {
            border = IdeaTitledBorder(RiderAzureBundle.message("dialog.create_sql_db.resource_group.header"), 0, JBUI.emptyInsets())
            add(cbResourceGroup, "growx")
        }

        pnlDatabaseEdition.apply {
            add(cbDatabaseEdition, "growx")
            add(lblDatabasePricingLink)
        }

        pnlSqlDatabaseSettings.apply {
            border = IdeaTitledBorder(RiderAzureBundle.message("dialog.create_sql_db.db_settings.header"), 0, JBUI.emptyInsets())
            add(lblSqlServer)
            add(cbSqlServer, "growx")

            add(lblDatabaseEdition)
            add(pnlDatabaseEdition, "growx")

            add(lblDatabaseComputeSize)
            add(cbDatabaseComputeSize, "growx")

            add(lblDatabaseCollation)
            add(txtDatabaseCollation, "growx")
        }

        add(pnlName, "growx")
        add(pnlSubscription, "growx")
        add(pnlResourceGroup, "growx")
        add(pnlSqlDatabaseSettings, "growx")

        initSubscriptionsComboBox()
        initResourceGroupComboBox()
        initSqlServerComboBox()
        initDatabaseEditionsComboBox()
        initDatabaseComputeSizeComboBox()

        initComponentValidation()
    }

    fun fillSubscriptions(subscriptions: List<Subscription>, serverSubscriptionId: String) {
        val subscription = subscriptions.find { it.subscriptionId() == serverSubscriptionId }
        pnlSubscription.cbSubscription.fillComboBox(listOf(subscription))
    }

    fun fillResourceGroups(resourceGroups: List<ResourceGroup>, serverResourceGroupname: String) {
        val resourceGroup = resourceGroups.find { it.name() == serverResourceGroupname }
        cbResourceGroup.fillComboBox(listOf(resourceGroup))
    }

    fun fillDatabaseEditions(editions: List<DatabaseEdition>) {
        val availableEditions = listOf(
                DatabaseEdition.BASIC,
                DatabaseEdition.STANDARD,
                DatabaseEdition.PREMIUM,
                DatabaseEdition.PREMIUM_RS,
                DatabaseEdition.DATA_WAREHOUSE,
                DatabaseEdition.STRETCH)

        val filteredEditions =
                editions.filter { availableEditions.contains(it) }.sortedWith(compareBy { it.toString() })

        cbDatabaseEdition.fillComboBox(filteredEditions, AzureDefaults.databaseEdition)
    }

    fun fillDatabaseComputeSize(objectives: List<ServiceObjectiveName>) {
        cachedComputeSize = objectives
        cbDatabaseComputeSize.fillComboBox(
                filterComputeSizeValues(objectives, cbDatabaseEdition.getSelectedValue() ?: AzureDefaults.databaseEdition))
    }

    override fun validateComponent(): List<ValidationInfo> {
        return listOfNotNull(
                SqlDatabaseValidator.validateDatabaseName(pnlName.txtNameValue.text)
                        .merge(SqlDatabaseValidator.checkSqlDatabaseExists(
                                pnlSubscription.lastSelectedSubscriptionId,
                                pnlName.txtNameValue.text,
                                cbSqlServer.getSelectedValue()?.name() ?: ""))
                        .toValidationInfo(pnlName.txtNameValue),
                SqlDatabaseValidator.checkEditionIsSet(cbDatabaseEdition.getSelectedValue()).toValidationInfo(cbDatabaseEdition),
                SqlDatabaseValidator.checkComputeSizeIsSet(cbDatabaseComputeSize.getSelectedValue()).toValidationInfo(cbDatabaseComputeSize),
                SqlDatabaseValidator.checkCollationIsSet(txtDatabaseCollation.text).toValidationInfo(txtDatabaseCollation)
        )
    }

    override fun initComponentValidation() {
        pnlName.txtNameValue.initValidationWithResult(
                lifetime,
                textChangeValidationAction = { SqlDatabaseValidator.checkInvalidCharacters(pnlName.txtNameValue.text) },
                focusLostValidationAction = { ValidationResult() })
    }

    private fun initSubscriptionsComboBox() {
        setComponentsEnabled(false, pnlSubscription.cbSubscription)
    }

    private fun initResourceGroupComboBox() {
        cbResourceGroup.setDefaultRenderer(RiderAzureBundle.message("dialog.create_sql_db.resource_group.empty_message")) { it.name() }
        setComponentsEnabled(false, cbResourceGroup)
    }

    private fun initSqlServerComboBox() {
        cbSqlServer.setDefaultRenderer("") { "${it.name()} (${it.resourceGroupName()})" }
        cbSqlServer.apply {
            removeAllItems()
            addItem(sqlServer)
        }

        setComponentsEnabled(false, cbSqlServer)
    }

    private fun initDatabaseEditionsComboBox() {
        cbDatabaseEdition.setDefaultRenderer(RiderAzureBundle.message("dialog.create_sql_db.db_edition.empty_message")) { it.toString() }
        cbDatabaseEdition.apply {
            addActionListener {
                val edition = cbDatabaseEdition.getSelectedValue() ?: return@addActionListener
                if (edition == lastSelectedDatabaseEdition) return@addActionListener
                lastSelectedDatabaseEdition = edition

                val filteredComputeSize = filterComputeSizeValues(cachedComputeSize, edition)
                cbDatabaseComputeSize.fillComboBox(filteredComputeSize)
            }
        }
    }

    private fun initDatabaseComputeSizeComboBox() {
        cbDatabaseComputeSize.setDefaultRenderer(RiderAzureBundle.message("dialog.create_sql_db.compute_size.empty_message")) { it.toString() }
    }

    private fun filterComputeSizeValues(objectives: List<ServiceObjectiveName>,
                                        edition: DatabaseEdition): List<ServiceObjectiveName> {

        if (edition == DatabaseEdition.BASIC)
            return listOf(ServiceObjectiveName.BASIC)

        val regex = when (edition) {
            DatabaseEdition.STANDARD -> Regex("^S(\\d+)")
            DatabaseEdition.PREMIUM -> Regex("^P(\\d+)")
            DatabaseEdition.PREMIUM_RS -> Regex("^PRS(\\d+)")
            DatabaseEdition.DATA_WAREHOUSE -> Regex("^DW(\\d+)c?")
            DatabaseEdition.STRETCH -> Regex("^DS(\\d+)")
            else -> return objectives
        }

        return objectives.filter { it.toString().matches(regex) }
                .sortedBy { regex.matchEntire(it.toString())?.groups?.get(1)?.value?.toIntOrNull() }
    }

}