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

package com.microsoft.intellij.ui.forms.appservice.webapp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import com.jetbrains.rd.util.threading.SpinWait
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.defaults.AzureDefaults
import com.microsoft.intellij.helpers.validator.ResourceGroupValidator
import com.microsoft.intellij.helpers.validator.WebAppValidator
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.config.ui.WebAppCreateNewComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.forms.base.AzureCreateDialogBase
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.util.Date
import java.time.Duration
import javax.swing.JComponent
import javax.swing.JPanel

class CreateWebAppDialog(private val lifetimeDef: LifetimeDefinition,
                         private val project: Project,
                         private val onCreate: Runnable = Runnable { }) :
        AzureCreateDialogBase(lifetimeDef, project),
        CreateWebAppMvpView {

    companion object {
        private val dialogTitle = message("dialog.create_web_app.title")
        private val webAppCreateTimeout: Duration = Duration.ofMinutes(1)

        private const val WEB_APP_HELP_URL = "https://azure.microsoft.com/en-us/services/app-service/web/"
    }

    private val panel = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1", ":$dialogMinWidth:"))

    private val pnlCreate = WebAppCreateNewComponent(lifetime)

    private val presenter = CreateWebAppViewPresenter<CreateWebAppDialog>()
    private val activityNotifier = AzureDeploymentProgressNotification(project)

    init {
        title = dialogTitle
        myPreferredFocusedComponent = pnlCreate.pnlAppName.txtAppName

        updateAzureModelInBackground(project)
        initSubscriptionComboBox()
        initMainPanel()

        presenter.onAttachView(this)

        init()
    }

    override val azureHelpUrl = WEB_APP_HELP_URL

    override fun createCenterPanel(): JComponent? = panel

    override fun doOKAction() {

        val appName = pnlCreate.pnlAppName.appName
        val subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId
        val resourceGroupName =
                when {
                    pnlCreate.pnlResourceGroup.isCreateNew -> pnlCreate.pnlResourceGroup.resourceGroupName
                    else -> pnlCreate.pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
                }

        val os =
                if (pnlCreate.pnlOperatingSystem.isWindows) OperatingSystem.WINDOWS
                else OperatingSystem.LINUX

        val progressMessage = message("dialog.create_web_app.creating", appName)

        var appId: String? = null

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, progressMessage, true) {

            override fun run(progress: ProgressIndicator) {
                val webApp = AzureDotNetWebAppMvpModel.createWebApp(
                        subscriptionId = subscriptionId,
                        appName = appName,
                        isCreatingResourceGroup = pnlCreate.pnlResourceGroup.isCreateNew,
                        resourceGroupName = resourceGroupName,
                        operatingSystem = os,
                        isCreatingAppServicePlan = pnlCreate.pnlAppServicePlan.isCreatingNew,
                        appServicePlanId = pnlCreate.pnlAppServicePlan.lastSelectedAppServicePlan?.id(),
                        appServicePlanName = pnlCreate.pnlAppServicePlan.servicePlanName,
                        pricingTier = pnlCreate.pnlAppServicePlan.cbPricingTier.getSelectedValue() ?: AzureDefaults.pricingTier,
                        location = pnlCreate.pnlAppServicePlan.location ?: AzureDefaults.location,
                        netFrameworkVersion = null,
                        netCoreRuntime = null
                )

                appId = webApp.id()
            }

            override fun onSuccess() {
                super.onSuccess()

                activityNotifier.notifyProgress(
                        message("tool_window.azure_activity_log.publish.web_app.create"),
                        Date(),
                        null,
                        100,
                        message("dialog.create_web_app.create_success", appName)
                )

                SpinWait.spinUntil(lifetimeDef, webAppCreateTimeout) {
                    val webAppId = appId ?: return@spinUntil false
                    val webApp =
                            try { AzureDotNetWebAppMvpModel.getWebAppById(subscriptionId, webAppId) }
                            catch (t: Throwable) { null }

                    webApp != null
                }

                application.invokeLater { onCreate.run() }
            }
        })

        super.doOKAction()
    }

    override fun validateComponent() =
            pnlCreate.pnlSubscription.validateComponent() +
                    pnlCreate.pnlResourceGroup.validateComponent() +
                    pnlCreate.pnlAppServicePlan.validateComponent() +
                    pnlCreate.pnlOperatingSystem.validateComponent() +
                    listOfNotNull(
                            WebAppValidator
                                    .validateWebAppName(pnlCreate.pnlAppName.appName)
                                    .merge(WebAppValidator.checkWebAppExists(pnlCreate.pnlAppName.appName))
                                    .toValidationInfo(pnlCreate.pnlAppName.txtAppName)
                    ) +
                    listOfNotNull(
                            if (pnlCreate.pnlResourceGroup.isCreateNew) {
                                ResourceGroupValidator.checkResourceGroupNameExists(
                                        subscriptionId = pnlCreate.pnlSubscription.lastSelectedSubscriptionId,
                                        name = pnlCreate.pnlResourceGroup.resourceGroupName
                                ).toValidationInfo(pnlCreate.pnlResourceGroup.txtResourceGroupName)
                            } else null
                    )

    override fun dispose() {
        presenter.onDetachView()
        super.dispose()
    }

    override fun fillSubscription(subscriptions: List<Subscription>) = pnlCreate.fillSubscription(subscriptions)

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) = pnlCreate.fillResourceGroup(resourceGroups)

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) = pnlCreate.fillAppServicePlan(appServicePlans)

    override fun fillLocation(locations: List<Location>) = pnlCreate.fillLocation(locations)

    override fun fillPricingTier(pricingTiers: List<PricingTier>) = pnlCreate.fillPricingTier(pricingTiers)

    private fun updateAzureModelInBackground(project: Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()
            }
        })
    }

    private fun initSubscriptionComboBox() {
        pnlCreate.pnlSubscription.listenerAction = { subscription ->
            val subscriptionId = subscription.subscriptionId()
            presenter.onLoadResourceGroups(lifetimeDef, subscriptionId)
            presenter.onLoadAppServicePlan(lifetimeDef, subscriptionId)
            presenter.onLoadLocation(lifetimeDef, subscriptionId)
        }
    }

    private fun initMainPanel() {
        panel.add(pnlCreate, "growx")

        UiNotifyConnector.Once(panel, object : Activatable {
            override fun showNotify() {
                presenter.onLoadSubscription(lifetimeDef)
                presenter.onLoadPricingTier(lifetimeDef)
            }
        })
    }
}
