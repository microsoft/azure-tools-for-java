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

package com.microsoft.intellij.ui.forms.sqlserver

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
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.forms.base.AzureCreateDialogBase
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.time.Duration
import java.util.Date
import javax.swing.JComponent
import javax.swing.JPanel

class CreateSqlServerDialog(private val lifetimeDef: LifetimeDefinition,
                            private val project: Project,
                            private val onCreate: Runnable = Runnable { }) :
        AzureCreateDialogBase(lifetimeDef, project),
        CreateSqlServerMvpView {

    companion object {
        private val dialogTitle = message("dialog.create_sql_server.title")
        private val sqlServerCreateTimeout: Duration = Duration.ofMinutes(2)
    }

    private val panel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1", ":$dialogMinWidth:"))

    private val pnlCreate = SqlServerCreateNewComponent(lifetimeDef)

    private val presenter = CreateSqlServerViewPresenter<CreateSqlServerDialog>()

    private val activityNotifier = AzureDeploymentProgressNotification()

    init {
        title = dialogTitle
        myPreferredFocusedComponent = pnlCreate.pnlName.txtNameValue

        updateAzureModelInBackground(project)

        initSubscriptionComboBox()
        initMainPanel()
        initComponentValidation()

        presenter.onAttachView(this)

        init()
    }

    override val azureHelpUrl = "https://azure.microsoft.com/en-us/services/sql-database/"

    override fun createCenterPanel(): JComponent = panel

    override fun validateComponent(): List<ValidationInfo> {
        val subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId
        return pnlCreate.pnlSubscription.validateComponent() +
                pnlCreate.pnlResourceGroup.validateComponent() +
                listOfNotNull(
                        SqlServerValidator
                                .validateSqlServerName(pnlCreate.serverName)
                                .merge(SqlServerValidator.checkSqlServerExistence(subscriptionId, pnlCreate.serverName))
                                .toValidationInfo(pnlCreate.pnlName.txtNameValue),

                        LocationValidator.checkLocationIsSet(pnlCreate.cbLocation.getSelectedValue())
                                .toValidationInfo(pnlCreate.cbLocation),

                        SqlServerValidator.validateAdminLogin(pnlCreate.txtAdminLoginValue.text)
                                .toValidationInfo(pnlCreate.txtAdminLoginValue),

                        SqlServerValidator.validateAdminPassword(pnlCreate.txtAdminLoginValue.text, pnlCreate.passAdminPassword.password)
                                .toValidationInfo(pnlCreate.passAdminPassword),

                        SqlServerValidator.checkPasswordsMatch(pnlCreate.passAdminPassword.password, pnlCreate.passAdminPasswordConfirm.password)
                                .toValidationInfo(pnlCreate.passAdminPasswordConfirm)
                ) +
                listOfNotNull(
                        if (pnlCreate.pnlResourceGroup.isCreateNew) {
                            ResourceGroupValidator.checkResourceGroupNameExists(subscriptionId, pnlCreate.pnlResourceGroup.resourceGroupName)
                                    .toValidationInfo(pnlCreate.pnlResourceGroup.txtResourceGroupName)
                        } else null
                )
    }

    override fun initComponentValidation() {

        pnlCreate.pnlName.txtNameValue.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkSqlServerNameMaxLength(pnlCreate.serverName)
                        .merge(SqlServerValidator.checkInvalidCharacters(pnlCreate.serverName)) },
                focusLostValidationAction = { SqlServerValidator.checkStartsEndsWithDash(pnlCreate.serverName) })

        pnlCreate.txtAdminLoginValue.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkLoginInvalidCharacters(pnlCreate.adminLogin) },
                focusLostValidationAction = { SqlServerValidator.checkRestrictedLogins(pnlCreate.adminLogin) })

        pnlCreate.passAdminPassword.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkPasswordContainsUsername(pnlCreate.adminPassword, pnlCreate.adminLogin) },
                focusLostValidationAction = { SqlServerValidator.checkPasswordRequirements(pnlCreate.adminPassword).merge(
                        if (pnlCreate.adminPassword.isEmpty()) ValidationResult()
                        else SqlServerValidator.checkPasswordMinLength(pnlCreate.adminPassword)) })

        pnlCreate.passAdminPasswordConfirm.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { ValidationResult() },
                focusLostValidationAction = { SqlServerValidator.checkPasswordsMatch(pnlCreate.adminPassword, pnlCreate.passAdminPasswordConfirm.password) })
    }

    override fun doOKAction() {
        val sqlServerName = pnlCreate.serverName
        val subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId
        val progressMessage = message("dialog.create_sql_server.creating", sqlServerName)
        val resourceGroup =
                when {
                    pnlCreate.pnlResourceGroup.isCreateNew -> pnlCreate.pnlResourceGroup.resourceGroupName
                    else -> pnlCreate.pnlResourceGroup.cbResourceGroup.getSelectedValue()?.name() ?: ""
                }

        var sqlServerId: String? = null

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                val server = AzureSqlServerMvpModel.createSqlServer(
                        subscriptionId          = subscriptionId,
                        sqlServerName           = sqlServerName,
                        region                  = pnlCreate.cbLocation.getSelectedValue()?.region() ?: AzureDefaults.location,
                        isCreatingResourceGroup = pnlCreate.pnlResourceGroup.isCreateNew,
                        resourceGroupName       = resourceGroup,
                        sqlServerAdminLogin     = pnlCreate.adminLogin,
                        sqlServerAdminPass      = pnlCreate.adminPassword
                )
                sqlServerId = server.id()
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        message("tool_window.azure_activity_log.publish.sql_server.create"),
                        Date(),
                        null,
                        100,
                        message("dialog.create_sql_server.create_success", sqlServerName)
                )

                SpinWait.spinUntil(lifetimeDef, sqlServerCreateTimeout) {
                    val serverId = sqlServerId ?: return@spinUntil false
                    val server =
                            try { AzureSqlServerMvpModel.getSqlServerById(subscriptionId, serverId) }
                            catch (t: Throwable) { null }
                    server != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun fillSubscription(subscriptions: List<Subscription>) = pnlCreate.fillSubscription(subscriptions)

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) = pnlCreate.fillResourceGroup(resourceGroups)

    override fun fillLocation(locations: List<Location>) = pnlCreate.fillLocation(locations)

    override fun dispose() {
        presenter.onDetachView()
        super.dispose()
    }

    private fun initMainPanel() {
        panel.add(pnlCreate, "growx")

        UiNotifyConnector.Once(panel, object : Activatable {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
            }
        })
    }

    private fun initSubscriptionComboBox() {
        pnlCreate.pnlSubscription.listenerAction = { subscription ->
            val subscriptionId = subscription.subscriptionId()
            presenter.onLoadResourceGroups(lifetimeDef, subscriptionId)
            presenter.onLoadLocation(lifetimeDef, subscriptionId)
        }
    }

    private fun updateAzureModelInBackground(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
            }
        })
    }
}
