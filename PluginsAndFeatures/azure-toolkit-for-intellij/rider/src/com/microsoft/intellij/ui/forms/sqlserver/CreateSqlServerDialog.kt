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

package com.microsoft.intellij.ui.forms.sqlserver

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SpinWait
import com.jetbrains.rider.util.idea.application
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.AzureResourceGroupSelector
import com.microsoft.intellij.ui.component.AzureResourceNameComponent
import com.microsoft.intellij.ui.component.AzureSubscriptionsSelector
import com.microsoft.intellij.ui.extension.setDefaultRenderer
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.initValidationWithResult
import com.microsoft.intellij.ui.extension.setComponentsEnabled
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.LocationValidator
import com.microsoft.intellij.helpers.validator.SqlServerValidator
import com.microsoft.intellij.helpers.validator.ValidationResult
import com.microsoft.intellij.ui.components.AzureDialogWrapper
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.awt.Dimension
import java.util.*
import javax.swing.*

class CreateSqlServerDialog(private val lifetimeDef: LifetimeDefinition,
                            private val project: Project,
                            private val onCreate: Runnable = Runnable { }) :
        AzureDialogWrapper(project),
        CreateSqlServerMvpView,
        AzureComponent {

    companion object {
        private const val DIALOG_MIN_WIDTH = 300
        private const val SQL_SERVER_CREATE_TIMEOUT_MS = 120_000L
        private const val AZURE_SQL_SERVER_HELP_URL = "https://azure.microsoft.com/en-us/services/sql-database/"
    }

    private val mainPanel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val pnlName = AzureResourceNameComponent()
    private val pnlSubscription = AzureSubscriptionsSelector()
    private val pnlResourceGroup = AzureResourceGroupSelector(lifetimeDef.createNested())

    private val pnlSqlServerSettings = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2", "[min!][]"))
    private val lblLocation = JLabel(message("dialog.create_sql_server.location.label"))
    private val cbLocation = ComboBox<Location>()
    private val lblAdminLogin = JLabel(message("dialog.create_sql_server.admin_login.label"))
    private val txtAdminLoginValue = JTextField()
    private val lblAdminPassword = JLabel(message("dialog.create_sql_server.admin_password.label"))
    private val passAdminPassword = JPasswordField()
    private val lblAdminPasswordConfirm = JLabel(message("dialog.create_sql_server.confirm_password.label"))
    private val passAdminPasswordConfirm = JPasswordField()

    private var cachedResourceGroups = emptyList<ResourceGroup>()

    private val presenter = CreateSqlServerViewPresenter<CreateSqlServerDialog>()
    private val activityNotifier = AzureDeploymentProgressNotification(project)

    init {
        title = message("dialog.create_sql_server.title")
        setOKButtonText(message("dialog.create_sql_server.ok_button.labe"))

        updateAzureModelInBackground(project)
        initSubscriptionComboBox()
        initLocationComboBox()
        initMainPanel()
        initComponentValidation()

        myPreferredFocusedComponent = pnlName.txtNameValue

        presenter.onAttachView(this)

        init()
    }

    override fun getPreferredSize() = Dimension(DIALOG_MIN_WIDTH, -1)

    override fun createCenterPanel(): JComponent = mainPanel

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
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkSqlServerNameMaxLength(pnlName.txtNameValue.text)
                        .merge(SqlServerValidator.checkInvalidCharacters(pnlName.txtNameValue.text)) },
                focusLostValidationAction = { SqlServerValidator.checkStartsEndsWithDash(pnlName.txtNameValue.text) })

        txtAdminLoginValue.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkLoginInvalidCharacters(txtAdminLoginValue.text) },
                focusLostValidationAction = { SqlServerValidator.checkRestrictedLogins(txtAdminLoginValue.text) })

        passAdminPassword.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { SqlServerValidator.checkPasswordContainsUsername(passAdminPassword.password, txtAdminLoginValue.text) },
                focusLostValidationAction = { SqlServerValidator.checkPasswordRequirements(passAdminPassword.password).merge(
                        if (passAdminPassword.password.isEmpty()) ValidationResult()
                        else SqlServerValidator.checkPasswordMinLength(passAdminPassword.password)) })

        passAdminPasswordConfirm.initValidationWithResult(
                lifetimeDef,
                textChangeValidationAction = { ValidationResult() },
                focusLostValidationAction = { SqlServerValidator.checkPasswordsMatch(passAdminPassword.password, passAdminPasswordConfirm.password) })
    }

    override fun doValidateAll() = validateComponent()

    override fun doOKAction() {
        val sqlServerName = pnlName.txtNameValue.text
        val subscriptionId = pnlSubscription.lastSelectedSubscriptionId
        val progressMessage = message("dialog.create_sql_server.creating", sqlServerName)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                AzureSqlServerMvpModel.createSqlServer(
                        subscriptionId,
                        sqlServerName,
                        cbLocation.getSelectedValue()!!.region(),
                        pnlResourceGroup.rdoCreateNew.isSelected,
                        when {
                            pnlResourceGroup.rdoCreateNew.isSelected -> pnlResourceGroup.txtResourceGroupName.text
                            else -> pnlResourceGroup.cbResourceGroup.getSelectedValue()!!.name()
                        },
                        txtAdminLoginValue.text,
                        passAdminPassword.password)
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

                SpinWait.spinUntil(lifetimeDef, SQL_SERVER_CREATE_TIMEOUT_MS) {
                    AzureSqlServerMvpModel.getSqlServerByName(subscriptionId, sqlServerName, true) != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun doHelpAction() {
        BrowserUtil.open(AZURE_SQL_SERVER_HELP_URL)
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        pnlSubscription.cbSubscription.removeAllItems()

        subscriptions.sortedWith(compareBy { it.displayName() }).forEach { subscription ->
            pnlSubscription.cbSubscription.addItem(subscription)
        }

        if (subscriptions.isEmpty()) {
            pnlSubscription.lastSelectedSubscriptionId = ""
        }
        setComponentsEnabled(true, pnlSubscription.cbSubscription)
    }

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
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

    override fun fillLocation(locations: List<Location>) {
        cbLocation.removeAllItems()

        locations.sortedWith(compareBy { it.displayName() }).forEach { location ->
            cbLocation.addItem(location)
            if (location.region() == AzureDefaults.location)
                cbLocation.selectedItem = location
        }
    }

    override fun dispose() {
        presenter.onDetachView()
        lifetimeDef.terminate()
        super.dispose()
    }

    private fun initMainPanel() {

        pnlResourceGroup.apply {
            border = IdeaTitledBorder(message("dialog.create_sql_server.resource_group.header"), 0, JBUI.emptyInsets())
        }

        pnlSqlServerSettings.apply {
            border = IdeaTitledBorder(message("dialog.create_sql_server.server_settings.header"), 0, JBUI.emptyInsets())

            add(lblLocation, "gapbefore 3")
            add(cbLocation, "growx")

            add(lblAdminLogin, "gapbefore 3")
            add(txtAdminLoginValue, "growx")

            add(lblAdminPassword, "gapbefore 3")
            add(passAdminPassword, "growx")

            add(lblAdminPasswordConfirm, "gapbefore 3")
            add(passAdminPasswordConfirm, "growx")
        }

        mainPanel.apply {
            add(pnlName, "growx, wmin $DIALOG_MIN_WIDTH")
            add(pnlSubscription, "growx")
            add(pnlResourceGroup, "growx")
            add(pnlSqlServerSettings, "growx")
        }

        UiNotifyConnector.Once(mainPanel, object : Activatable.Adapter() {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
            }
        })
    }

    private fun initSubscriptionComboBox() {
        pnlSubscription.listenerAction = { subscription ->
            val subscriptionId = subscription.subscriptionId()
            presenter.onLoadResourceGroups(lifetimeDef, subscriptionId)
            presenter.onLoadLocation(lifetimeDef, subscriptionId)

            pnlResourceGroup.subscriptionId = subscriptionId
        }
    }

    private fun initLocationComboBox() {
        cbLocation.setDefaultRenderer(message("dialog.create_sql_server.location.empty_message")) { it.displayName() }
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
