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

package com.microsoft.intellij.ui.forms.appservice.functionapp

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
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.FunctionAppValidator
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.StorageAccountValidator
import com.microsoft.intellij.runner.functionapp.config.ui.FunctionAppCreateNewComponent
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.forms.base.AzureCreateDialogBase
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.time.Duration
import java.util.Date
import javax.swing.JComponent
import javax.swing.JPanel

class CreateFunctionAppDialog(lifetimeDef: LifetimeDefinition,
                              project: Project,
                              private val onCreate: Runnable = Runnable { }) :
        AzureCreateDialogBase(lifetimeDef, project),
        CreateFunctionAppMvpView {

    companion object {
        private val dialogTitle: String = message("dialog.create_function_app.title")
        private val functionAppCreateTimeout: Duration = Duration.ofMinutes(1)

        private const val FUNCTION_APP_HELP_URL = "https://azure.microsoft.com/en-us/services/functions/"
    }

    private val panel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1", ":$dialogMinWidth:"))

    private val pnlCreate = FunctionAppCreateNewComponent(lifetime)

    private val presenter = CreateFunctionAppViewPresenter<CreateFunctionAppDialog>()

    private val activityNotifier = AzureDeploymentProgressNotification()

    init {
        title = dialogTitle
        myPreferredFocusedComponent = pnlCreate.pnlAppName.txtAppName

        updateAzureModelInBackground(project)
        initSubscriptionComboBox()
        initMainPanel()

        presenter.onAttachView(this)

        init()
    }

    override val azureHelpUrl: String = FUNCTION_APP_HELP_URL

    override fun createCenterPanel(): JComponent? = panel

    override fun doOKAction() {

        val appName = pnlCreate.pnlAppName.appName
        val subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId
        val resourceGroupName =
                when {
                    pnlCreate.pnlResourceGroup.isCreateNew -> pnlCreate.pnlResourceGroup.resourceGroupName
                    else -> pnlCreate.pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
                }

        val progressMessage = message("dialog.create_function_app.creating", appName)

        var appId: String? = null

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                val app = AzureFunctionAppMvpModel.createFunctionApp(
                        subscriptionId         = subscriptionId,
                        appName                = appName,
                        isCreateResourceGroup  = pnlCreate.pnlResourceGroup.isCreateNew,
                        resourceGroupName      = resourceGroupName,
                        isCreateAppServicePlan = pnlCreate.pnlHostingPlan.isCreatingNew,
                        appServicePlanId       = pnlCreate.pnlHostingPlan.lastSelectedAppServicePlan?.id() ?: "",
                        appServicePlanName     = pnlCreate.pnlHostingPlan.hostingPlanName,
                        pricingTier            = pnlCreate.pnlHostingPlan.cbPricingTier.getSelectedValue() ?: FunctionAppPublishModel.defaultPricingTier,
                        region                 = pnlCreate.pnlHostingPlan.cbLocation.getSelectedValue()?.region() ?: AzureDefaults.location,
                        isCreateStorageAccount = pnlCreate.pnlStorageAccount.isCreatingNew,
                        storageAccountId       = pnlCreate.pnlStorageAccount.lastSelectedStorageAccount?.id() ?: "",
                        storageAccountName     = pnlCreate.pnlStorageAccount.storageAccountName,
                        storageAccountType     = pnlCreate.pnlStorageAccount.storageAccountType ?: FunctionAppPublishModel.defaultStorageAccountType
                )

                appId = app.id()
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        message("tool_window.azure_activity_log.publish.function_app.create"),
                        Date(),
                        null,
                        100,
                        message("dialog.create_function_app.create_success", appName)
                )

                SpinWait.spinUntil(lifetimeDef, functionAppCreateTimeout) {
                    val functionAppId = appId ?: return@spinUntil false
                    val functionApp =
                            try { AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, functionAppId) }
                            catch (t: Throwable) { null }
                    functionApp != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun validateComponent(): List<ValidationInfo> {
        val subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId
        return pnlCreate.pnlSubscription.validateComponent() +
                pnlCreate.pnlResourceGroup.validateComponent() +
                pnlCreate.pnlHostingPlan.validateComponent() +
                pnlCreate.pnlStorageAccount.validateComponent() +
                listOfNotNull(
                        FunctionAppValidator.validateFunctionAppName(subscriptionId, pnlCreate.pnlAppName.appName)
                                .toValidationInfo(pnlCreate.pnlAppName.txtAppName)
                ) +
                listOfNotNull(
                        if (pnlCreate.pnlResourceGroup.isCreateNew) {
                            ResourceGroupValidator.checkResourceGroupNameExists(subscriptionId, pnlCreate.pnlResourceGroup.resourceGroupName)
                                    .toValidationInfo(pnlCreate.pnlResourceGroup.txtResourceGroupName)
                        } else null
                ) +
                listOfNotNull(
                        if (pnlCreate.pnlStorageAccount.isCreatingNew) {
                            StorageAccountValidator.checkStorageAccountNameExists(subscriptionId, pnlCreate.pnlStorageAccount.storageAccountName)
                                    .toValidationInfo(pnlCreate.pnlStorageAccount.txtName)
                        } else null
                )
    }

    override fun dispose() {
        presenter.onDetachView()
        super.dispose()
    }


    override fun fillSubscription(subscriptions: List<Subscription>) = pnlCreate.fillSubscription(subscriptions)

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) = pnlCreate.fillResourceGroup(resourceGroups)

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) = pnlCreate.fillAppServicePlan(appServicePlans)

    override fun fillLocation(locations: List<Location>) = pnlCreate.fillLocation(locations)

    override fun fillPricingTier(pricingTiers: List<PricingTier>) = pnlCreate.fillPricingTier(pricingTiers)

    override fun fillStorageAccount(storageAccounts: List<StorageAccount>) = pnlCreate.fillStorageAccount(storageAccounts)

    override fun fillStorageAccountType(storageAccountTypes: List<StorageAccountSkuType>) = pnlCreate.fillStorageAccountType(storageAccountTypes)


    private fun updateAzureModelInBackground(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureFunctionAppMvpModel.refreshSubscriptionToFunctionAppMap()
                AzureStorageAccountMvpModel.refreshStorageAccountsMap()
            }
        })
    }

    private fun initSubscriptionComboBox() {
        pnlCreate.pnlSubscription.listenerAction = { subscription ->
            val subscriptionId = subscription.subscriptionId()
            presenter.onLoadResourceGroups(lifetimeDef, subscriptionId)
            presenter.onLoadAppServicePlan(lifetimeDef, subscriptionId)
            presenter.onLoadLocation(lifetimeDef, subscriptionId)
            presenter.onLoadStorageAccounts(lifetime, subscriptionId)
        }
    }

    private fun initMainPanel() {
        panel.add(pnlCreate, "growx")

        UiNotifyConnector.Once(panel, object : Activatable {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
                presenter.onLoadPricingTier(lifetimeDef)
                presenter.onLoadStorageAccountTypes(lifetimeDef)
            }
        })
    }
}
