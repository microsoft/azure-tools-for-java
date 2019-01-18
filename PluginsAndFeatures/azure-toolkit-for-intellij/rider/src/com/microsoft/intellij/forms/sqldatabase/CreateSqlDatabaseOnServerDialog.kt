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

package com.microsoft.intellij.forms.sqldatabase

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SpinWait
import com.jetbrains.rider.util.idea.application
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.*
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.AzureResourceNameComponent
import com.microsoft.intellij.component.AzureSubscriptionsSelector
import com.microsoft.intellij.component.extension.*
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.deploy.NotificationConstant
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.SubscriptionValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.ui.components.AzureDialogWrapper
import net.miginfocom.swing.MigLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class CreateSqlDatabaseOnServerDialog(private val lifetimeDef: LifetimeDefinition,
                                      private val project: Project,
                                      private val sqlServer: SqlServer,
                                      private val onCreate: Runnable = Runnable {  }) :
        AzureDialogWrapper(project),
        CreateSqlDatabaseOnServerMvpView,
        AzureComponent {

    companion object {
        private const val DIALOG_TITLE = "Create SQL Database"
        private const val DIALOG_OK_BUTTON_TEXT = "Create"
        private const val DIALOG_MIN_WIDTH = 200

        private const val EMPTY_RESOURCE_GROUP_MESSAGE = "No existing Resource Groups"
        private const val EMPTY_DATABASE_EDITIONS_MESSAGE = "No existing Azure Database Editions"
        private const val EMPTY_COMPUTE_SIZE_MESSAGE = "No existing Azure Compute Sizes"

        private const val TITLE_RESOURCE_GROUP = "Resource Group"
        private const val TITLE_SQL_DATABASE_SETTINGS = "Database Settings"

        private const val SQL_DATABASE_CREATING_MESSAGE = "Creating '%s' database..."
        private const val SQL_DATABASE_CREATE_SUCCESSFUL = "SQL Database is created, id: '%s'"

        private const val AZURE_SQL_DATABASE_HELP_URL = "https://azure.microsoft.com/en-us/services/sql-database/"
        private const val AZURE_SQL_DATABASE_PRICING_URI = "https://azure.microsoft.com/en-us/pricing/details/sql-database/"

        private const val SQL_DATABASE_CREATE_TIMEOUT_MS = 120_000L
    }

    private val mainPanel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val pnlName = AzureResourceNameComponent()
    private val pnlSubscription = AzureSubscriptionsSelector()
    private val pnlResourceGroup = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    private val cbResourceGroup = ComboBox<ResourceGroup>()

    private val pnlSqlDatabaseSettings = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]"))
    private val lblSqlServer = JLabel("SQL Server")
    private val cbSqlServer = ComboBox<SqlServer>()

    private val lblDatabaseEdition = JLabel("Edition")
    private val pnlDatabaseEdition = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[][min!]"))
    private val cbDatabaseEdition = ComboBox<DatabaseEditions>()
    private val lblDatabasePricingLink = LinkLabel("Pricing", null, { _, link -> BrowserUtil.browse(link)}, AZURE_SQL_DATABASE_PRICING_URI)

    private val lblDatabaseComputeSize = JLabel("Compute size")
    private val cbDatabaseComputeSize = ComboBox<ServiceObjectiveName>()

    private val lblDatabaseCollation = JLabel("Collation")
    private val txtDatabaseCollation = JTextField(AzureDefaults.SQL_DATABASE_COLLATION)

    private val presenter = CreateSqlDatabaseOnServerViewPresenter<CreateSqlDatabaseOnServerDialog>()
    private val activityNotifier = AzureDeploymentProgressNotification(project)

    private var lastSelectedDatabaseEdition: DatabaseEditions = DatabaseEditions()
    private var cachedComputeSize = listOf<ServiceObjectiveName>()

    init {
        title = DIALOG_TITLE
        setOKButtonText(DIALOG_OK_BUTTON_TEXT)

        initSubscriptionsComboBox()
        initResourceGroupComboBox()
        initSqlServerComboBox()
        initDatabaseEditionsComboBox()
        initDatabaseComputeSizeComboBox()
        initMainPanel()
        initComponentValidation()

        presenter.onAttachView(this)

        init()
    }

    override fun createCenterPanel(): JComponent? = mainPanel

    override fun fillResourceGroups(resourceGroups: List<ResourceGroup>) {
        val resourceGroup = resourceGroups.find { it.name() == sqlServer.resourceGroupName() }
        cbResourceGroup.fillComboBox(listOf(resourceGroup))
    }

    override fun fillDatabaseEditions(editions: List<DatabaseEditions>) {

        val availableEditions = listOf(
                DatabaseEditions.BASIC,
                DatabaseEditions.STANDARD,
                DatabaseEditions.PREMIUM,
                DatabaseEditions.PREMIUM_RS,
                DatabaseEditions.DATA_WAREHOUSE,
                DatabaseEditions.STRETCH)

        val filteredEditions =
                editions.filter { availableEditions.contains(it) }.sortedWith(compareBy { it.toString() })

        cbDatabaseEdition.fillComboBox(filteredEditions, AzureDefaults.databaseEdition)
    }

    override fun fillDatabaseComputeSize(objectives: List<ServiceObjectiveName>) {
        cachedComputeSize = objectives
        cbDatabaseComputeSize.fillComboBox(
                filterComputeSizeValues(cachedComputeSize, cbDatabaseEdition.getSelectedValue() ?: AzureDefaults.databaseEdition))
    }

    override fun validateComponent(): List<ValidationInfo> {
        return listOfNotNull(
                SqlDatabaseValidator.validateDatabaseName(pnlName.txtNameValue.text)
                        .merge(SqlDatabaseValidator.checkSqlDatabaseExistence(
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
                lifetimeDef,
                textChangeValidationAction = { SqlDatabaseValidator.checkInvalidCharacters(pnlName.txtNameValue.text) },
                focusLostValidationAction = { ValidationResult() })
    }

    override fun fillSubscriptions(subscriptions: List<Subscription>) {
        val subscription = subscriptions.find { it.subscriptionId() == sqlServer.manager().subscriptionId() }
        pnlSubscription.cbSubscription.fillComboBox(listOf(subscription))
    }

    override fun doValidateAll(): List<ValidationInfo> {

        val subscription = pnlSubscription.cbSubscription.getSelectedValue()
        val subscriptionId = subscription?.subscriptionId() ?: ""
        val resourceGroup = cbResourceGroup.getSelectedValue()
        val sqlDatabaseName = pnlName.txtNameValue.text

        return listOfNotNull(
                validationSqlDatabaseName(),
                SubscriptionValidator.checkSubscriptionIsSet(subscription).toValidationInfo(pnlSubscription.cbSubscription),
                ResourceGroupValidator.checkResourceGroupIsSet(resourceGroup).toValidationInfo(cbResourceGroup),
                SqlDatabaseValidator.checkSqlDatabaseExistence(subscriptionId, sqlDatabaseName, sqlServer.name()).toValidationInfo(pnlName.txtNameValue),
                SqlDatabaseValidator.checkEditionIsSet(cbDatabaseEdition.getSelectedValue()).toValidationInfo(cbDatabaseEdition),
                SqlDatabaseValidator.checkComputeSizeIsSet(cbDatabaseComputeSize.getSelectedValue()).toValidationInfo(cbDatabaseComputeSize),
                SqlDatabaseValidator.checkCollationIsSet(txtDatabaseCollation.text).toValidationInfo(txtDatabaseCollation))
    }

    override fun doOKAction() {

        val sqlDatabaseName = pnlName.txtNameValue.text
        val progressMessage = String.format(SQL_DATABASE_CREATING_MESSAGE, sqlDatabaseName)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                AzureSqlDatabaseMvpModel.createSqlDatabase(
                        databaseName = sqlDatabaseName,
                        sqlServer = sqlServer,
                        collation = txtDatabaseCollation.text,
                        edition = cbDatabaseEdition.getSelectedValue()!!,
                        serviceObjectiveName = cbDatabaseComputeSize.getSelectedValue()!!)
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        NotificationConstant.SQL_DATABASE_CREATE, Date(), null, 100, String.format(SQL_DATABASE_CREATE_SUCCESSFUL, sqlDatabaseName))

                SpinWait.spinUntil(lifetimeDef, SQL_DATABASE_CREATE_TIMEOUT_MS) {
                    AzureSqlDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer, true)
                            .find { it.name() == sqlDatabaseName } != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun doHelpAction() {
        BrowserUtil.open(AZURE_SQL_DATABASE_HELP_URL)
    }

    override fun dispose() {
        presenter.onDetachView()
        lifetimeDef.terminate()
        super.dispose()
    }

    private fun initMainPanel() {
        pnlResourceGroup.apply {
            border = IdeaTitledBorder(TITLE_RESOURCE_GROUP, 0, JBUI.emptyInsets())
            add(cbResourceGroup, "growx")
        }

        pnlDatabaseEdition.apply {
            add(cbDatabaseEdition, "growx")
            add(lblDatabasePricingLink)
        }

        pnlSqlDatabaseSettings.apply {
            border = IdeaTitledBorder(TITLE_SQL_DATABASE_SETTINGS, 0, JBUI.emptyInsets())
            add(lblSqlServer)
            add(cbSqlServer, "growx")

            add(lblDatabaseEdition)
            add(pnlDatabaseEdition, "growx")

            add(lblDatabaseComputeSize)
            add(cbDatabaseComputeSize, "growx")

            add(lblDatabaseCollation)
            add(txtDatabaseCollation, "growx")
        }

        mainPanel.apply {
            add(pnlName, "growx, wmin $DIALOG_MIN_WIDTH")
            add(pnlSubscription, "growx")
            add(pnlResourceGroup, "growx")
            add(pnlSqlDatabaseSettings, "growx")
        }

        UiNotifyConnector.Once(mainPanel, object : Activatable.Adapter() {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
                presenter.onLoadComputeSize()
                presenter.onLoadDatabaseEditions()
            }
        })
    }

    private fun initSubscriptionsComboBox() {
        pnlSubscription.listenerAction = { subscription -> presenter.onLoadResourceGroups(lifetimeDef, subscription.subscriptionId()) }
        setComponentsEnabled(false, pnlSubscription.cbSubscription)
    }

    private fun initResourceGroupComboBox() {
        cbResourceGroup.renderer =
                cbResourceGroup.createDefaultRenderer(EMPTY_RESOURCE_GROUP_MESSAGE) { it.name() }

        setComponentsEnabled(false, cbResourceGroup)
    }

    private fun initSqlServerComboBox() {
        cbSqlServer.apply {
            renderer = cbSqlServer.createDefaultRenderer("") { "${it.name()} (${it.resourceGroupName()})" }

            removeAllItems()
            addItem(sqlServer)
        }

        setComponentsEnabled(false, cbSqlServer)
    }

    private fun initDatabaseEditionsComboBox() {
        cbDatabaseEdition.apply {
            renderer = cbDatabaseEdition.createDefaultRenderer(EMPTY_DATABASE_EDITIONS_MESSAGE) { it.toString() }

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
        cbDatabaseComputeSize.renderer =
                cbDatabaseComputeSize.createDefaultRenderer(EMPTY_COMPUTE_SIZE_MESSAGE) { it.toString() }
    }

    private fun validationSqlDatabaseName() =
            SqlDatabaseValidator.validateDatabaseName(pnlName.txtNameValue.text).toValidationInfo(pnlName.txtNameValue)

    private fun filterComputeSizeValues(objectives: List<ServiceObjectiveName>,
                                        edition: DatabaseEditions): List<ServiceObjectiveName> {

        if (edition == DatabaseEditions.BASIC)
            return listOf(ServiceObjectiveName.BASIC)

        val regex = when (edition) {
            DatabaseEditions.STANDARD -> Regex("^S(\\d+)")
            DatabaseEditions.PREMIUM -> Regex("^P(\\d+)")
            DatabaseEditions.PREMIUM_RS -> Regex("^PRS(\\d+)")
            DatabaseEditions.DATA_WAREHOUSE -> Regex("^DW(\\d+)c?")
            DatabaseEditions.STRETCH -> Regex("^DS(\\d+)")
            else -> return objectives
        }

        return objectives.filter { it.toString().matches(regex) }
                .sortedBy { regex.matchEntire(it.toString())?.groups?.get(1)?.value?.toIntOrNull() }
    }
}
