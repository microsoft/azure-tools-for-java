/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SpinWait
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.SqlDatabaseValidator
import com.microsoft.intellij.helpers.validator.SubscriptionValidator
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.forms.base.AzureCreateDialogBase
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.time.Duration
import java.util.Date
import javax.swing.JComponent
import javax.swing.JPanel

class CreateSqlDatabaseOnServerDialog(lifetimeDef: LifetimeDefinition,
                                      project: Project,
                                      private val sqlServer: SqlServer,
                                      private val onCreate: Runnable = Runnable {  }) :
        AzureCreateDialogBase(lifetimeDef, project),
        CreateSqlDatabaseOnServerMvpView {

    companion object {
        private val dialogTitle = message("dialog.create_sql_db.title")
        private val sqlDatabaseCreateTimeout: Duration = Duration.ofMinutes(2)

        private const val SQL_DATABASE_HELP_URL = "https://azure.microsoft.com/en-us/services/sql-database/"
    }

    private val panel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1", ":$dialogMinWidth:"))

    private val pnlCreate = SqlDatabaseCreateNewComponent(lifetimeDef, sqlServer)

    private val presenter = CreateSqlDatabaseOnServerViewPresenter<CreateSqlDatabaseOnServerDialog>()
    private val activityNotifier = AzureDeploymentProgressNotification()

    init {
        title = dialogTitle
        myPreferredFocusedComponent = pnlCreate.pnlName.txtNameValue

        initSubscriptionComboBox()
        initMainPanel()

        presenter.onAttachView(this)

        init()
    }

    override val azureHelpUrl = SQL_DATABASE_HELP_URL

    override fun createCenterPanel(): JComponent? = panel

    override fun fillSubscriptions(subscriptions: List<Subscription>) =
            pnlCreate.fillSubscriptions(
                    subscriptions = subscriptions,
                    serverSubscriptionId = sqlServer.manager().subscriptionId())

    override fun fillResourceGroups(resourceGroups: List<ResourceGroup>) =
            pnlCreate.fillResourceGroups(
                    resourceGroups = resourceGroups,
                    serverResourceGroupname = sqlServer.resourceGroupName())

    override fun fillDatabaseEditions(editions: List<DatabaseEdition>) =
            pnlCreate.fillDatabaseEditions(editions)

    override fun fillDatabaseComputeSize(objectives: List<ServiceObjectiveName>) =
            pnlCreate.fillDatabaseComputeSize(objectives)

    override fun validateComponent(): List<ValidationInfo> {
        val subscription = pnlCreate.pnlSubscription.cbSubscription.getSelectedValue()
        val subscriptionId = subscription?.subscriptionId() ?: ""
        val resourceGroup = pnlCreate.cbResourceGroup.getSelectedValue()
        val sqlDatabaseName = pnlCreate.databaseName

        return listOfNotNull(
                SqlDatabaseValidator.validateDatabaseName(pnlCreate.databaseName)
                        .toValidationInfo(pnlCreate.pnlName.txtNameValue),

                SubscriptionValidator.checkSubscriptionIsSet(subscription)
                        .toValidationInfo(pnlCreate.pnlSubscription.cbSubscription),

                ResourceGroupValidator.checkResourceGroupIsSet(resourceGroup)
                        .toValidationInfo(pnlCreate.cbResourceGroup),

                SqlDatabaseValidator.checkSqlDatabaseExists(subscriptionId, sqlDatabaseName, sqlServer.name())
                        .toValidationInfo(pnlCreate.pnlName.txtNameValue),

                SqlDatabaseValidator.checkEditionIsSet(pnlCreate.cbDatabaseEdition.getSelectedValue())
                        .toValidationInfo(pnlCreate.cbDatabaseEdition),

                SqlDatabaseValidator.checkComputeSizeIsSet(pnlCreate.cbDatabaseComputeSize.getSelectedValue())
                        .toValidationInfo(pnlCreate.cbDatabaseComputeSize),

                SqlDatabaseValidator.checkCollationIsSet(pnlCreate.txtDatabaseCollation.text)
                        .toValidationInfo(pnlCreate.txtDatabaseCollation))
    }

    override fun doOKAction() {

        val sqlDatabaseName = pnlCreate.databaseName
        val progressMessage = message("dialog.create_sql_db.creating", sqlDatabaseName)

        var databaseId: String? = null

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                val database = AzureSqlDatabaseMvpModel.createSqlDatabase(
                        databaseName         = sqlDatabaseName,
                        sqlServer            = sqlServer,
                        collation            = pnlCreate.collation,
                        edition              = pnlCreate.cbDatabaseEdition.getSelectedValue() ?: AzureDefaults.databaseEdition,
                        serviceObjectiveName = pnlCreate.cbDatabaseComputeSize.getSelectedValue() ?: AzureDefaults.databaseComputeSize
                )
                databaseId = database.id()
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        message("tool_window.azure_activity_log.publish.sql_db.create"),
                        Date(),
                        null,
                        100,
                        message("dialog.create_sql_db.create_success", sqlDatabaseName)
                )

                SpinWait.spinUntil(lifetimeDef, sqlDatabaseCreateTimeout) {
                    val database =
                            try { AzureSqlDatabaseMvpModel.listSqlDatabasesBySqlServer(sqlServer, true).find { db -> db.id() == databaseId } }
                            catch (t: Throwable) { null }

                    database != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun dispose() {
        presenter.onDetachView()
        super.dispose()
    }

    private fun initSubscriptionComboBox() {
        pnlCreate.pnlSubscription.listenerAction = { subscription ->
            presenter.onLoadResourceGroups(lifetimeDef, subscription.subscriptionId())
        }
    }

    private fun initMainPanel() {
        panel.add(pnlCreate, "growx")

        UiNotifyConnector.Once(panel, object : Activatable {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
                presenter.onLoadComputeSize()
                presenter.onLoadDatabaseEditions()
            }
        })
    }
}
