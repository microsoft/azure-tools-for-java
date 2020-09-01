/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.RandomStringUtils
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ExistingOrNewSelector
import com.microsoft.intellij.ui.component.PublishableProjectComponent
import com.microsoft.intellij.ui.component.appservice.AppAfterPublishSettingPanel
import com.microsoft.intellij.ui.component.appservice.AppExistingComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.setComponentsVisible
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JPanel

class FunctionAppPublishComponent(lifetime: Lifetime,
                                  project: Project,
                                  private val model: FunctionAppPublishModel) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val DEFAULT_APP_NAME = "functionapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"
    }

    private val pnlFunctionAppSelector = ExistingOrNewSelector(message("run_config.publish.form.function_app.existing_new_selector"))
    val pnlExistingFunctionApp = AppExistingComponent<FunctionApp>()
    val pnlCreateFunctionApp = FunctionAppCreateNewComponent(lifetime.createNested())
    private val pnlProject = PublishableProjectComponent(project)
    private val pnlFunctionAppPublishSettings = AppAfterPublishSettingPanel()

    init {
        initProjectPanel()

        add(pnlFunctionAppSelector)
        add(pnlExistingFunctionApp, "growx")
        add(pnlCreateFunctionApp, "growx")
        add(pnlProject, "growx")
        add(pnlFunctionAppPublishSettings, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    fun resetFromConfig(config: FunctionAppPublishModel, dateString: String) {
        if (config.publishableProject != null)
            pnlProject.cbProject.selectedItem = config.publishableProject

        pnlCreateFunctionApp.pnlAppName.txtAppName.text =
                if (config.appName.isEmpty()) "$DEFAULT_APP_NAME$dateString"
                else config.appName

        pnlCreateFunctionApp.pnlResourceGroup.txtResourceGroupName.text =
                if (config.resourceGroupName.isEmpty()) "$DEFAULT_RESOURCE_GROUP_NAME$dateString"
                else config.resourceGroupName

        pnlCreateFunctionApp.pnlHostingPlan.txtName.text =
                if (config.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString"
                else config.appServicePlanName

        pnlCreateFunctionApp.pnlStorageAccount.txtName.text =
                if (config.storageAccountName.isEmpty()) RandomStringUtils.randomAlphanumeric(4).toLowerCase()
                else config.storageAccountName

        // Is Creating New
        if (config.isCreatingNewApp) pnlFunctionAppSelector.rdoCreateNew.doClick()
        else pnlFunctionAppSelector.rdoUseExisting.doClick()

        // Resource Group
        if (config.isCreatingResourceGroup) pnlCreateFunctionApp.pnlResourceGroup.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlResourceGroup.rdoUseExisting.doClick()

        // Hosting Plan
        if (config.isCreatingAppServicePlan) pnlCreateFunctionApp.pnlHostingPlan.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlHostingPlan.rdoUseExisting.doClick()

        // Storage Account
        if (config.isCreatingStorageAccount) pnlCreateFunctionApp.pnlStorageAccount.rdoCreateNew.doClick()
        else pnlCreateFunctionApp.pnlStorageAccount.rdoUseExisting.doClick()

        // Settings
        val isOpenInBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        if (isOpenInBrowser)
            pnlFunctionAppPublishSettings.checkBoxOpenInBrowserAfterPublish.doClick()

        pnlExistingFunctionApp.pnlExistingAppTable.btnRefresh.isEnabled = false
    }

    fun applyConfig(model: FunctionAppPublishModel) {
        model.subscription = pnlCreateFunctionApp.pnlSubscription.cbSubscription.getSelectedValue()
        model.publishableProject = pnlProject.lastSelectedProject

        model.isCreatingNewApp = pnlFunctionAppSelector.isCreateNew
        model.appName = pnlCreateFunctionApp.pnlAppName.appName

        if (!model.isCreatingNewApp) {
            val selectedResource = pnlExistingFunctionApp.pnlExistingAppTable.lastSelectedResource
            val selectedApp = selectedResource?.resource

            model.appId = selectedApp?.id() ?: ""
            model.subscription = AzureMvpModel.getInstance()
                    .selectedSubscriptions
                    .find { it.subscriptionId() == selectedResource?.subscriptionId }
        }

        model.isCreatingResourceGroup = pnlCreateFunctionApp.pnlResourceGroup.isCreateNew
        if (pnlCreateFunctionApp.pnlResourceGroup.isCreateNew) {
            model.resourceGroupName = pnlCreateFunctionApp.pnlResourceGroup.resourceGroupName
        } else {
            model.resourceGroupName = pnlCreateFunctionApp.pnlResourceGroup.cbResourceGroup.getSelectedValue()?.name() ?: ""
        }

        val hostingPlan = pnlCreateFunctionApp.pnlHostingPlan
        model.isCreatingAppServicePlan = hostingPlan.isCreatingNew
        model.appServicePlanId         = hostingPlan.lastSelectedAppServicePlan?.id() ?: model.appServicePlanId
        model.appServicePlanName       = hostingPlan.hostingPlanName
        model.location                 = hostingPlan.cbLocation.getSelectedValue()?.region() ?: model.location
        model.pricingTier              = hostingPlan.cbPricingTier.getSelectedValue() ?: model.pricingTier

        val storageAccount = pnlCreateFunctionApp.pnlStorageAccount
        model.isCreatingStorageAccount = storageAccount.isCreatingNew
        model.storageAccountName       = storageAccount.storageAccountName
        model.storageAccountId         = storageAccount.cbStorageAccount.getSelectedValue()?.id() ?: ""
        model.storageAccountType       = storageAccount.cbStorageAccountType.getSelectedValue() ?: model.storageAccountType
    }

    //region Fill Values

    fun fillAppsTable(apps: List<ResourceEx<FunctionApp>>) =
            pnlExistingFunctionApp.fillAppsTable(apps, model.appId)

    fun fillSubscription(subscriptions: List<Subscription>) =
            pnlCreateFunctionApp.fillSubscription(subscriptions, model.subscription)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) =
            pnlCreateFunctionApp.fillResourceGroup(resourceGroups, model.resourceGroupName)

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) =
            pnlCreateFunctionApp.fillAppServicePlan(appServicePlans, model.appServicePlanId)

    fun fillLocation(locations: List<Location>) =
            pnlCreateFunctionApp.fillLocation(locations, model.location)

    fun fillPricingTier(pricingTiers: List<PricingTier>) =
            pnlCreateFunctionApp.fillPricingTier(pricingTiers, model.pricingTier)

    fun fillStorageAccount(storageAccounts: List<StorageAccount>) =
            pnlCreateFunctionApp.fillStorageAccount(storageAccounts, model.storageAccountId)

    fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>) =
            pnlCreateFunctionApp.fillStorageAccountType(storageAccountType)

    fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlProject.fillProjectComboBox(publishableProjects, model.publishableProject)

        if (model.publishableProject != null && publishableProjects.contains(model.publishableProject!!)) {
            pnlProject.lastSelectedProject = model.publishableProject
        }
    }

    //endregion Fill Values

    //region Project

    private fun initProjectPanel() {

        pnlProject.canBePublishedAction = { publishableProject -> publishableProject.isAzureFunction }

        pnlProject.listenerAction = { publishableProject ->
            pnlExistingFunctionApp.filterAppTableContent(publishableProject.isDotNetCore)
        }
    }

    //endregion Project

    //region Button Group

    private fun initButtonGroupsState() {
        initFunctionAppButtonGroup()
    }

    private fun initFunctionAppButtonGroup() {
        pnlFunctionAppSelector.rdoUseExisting.addActionListener { toggleFunctionAppPanel(false) }
        pnlFunctionAppSelector.rdoCreateNew.addActionListener { toggleFunctionAppPanel(true) }
        toggleFunctionAppPanel(model.isCreatingNewApp)
    }

    private fun toggleFunctionAppPanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateFunctionApp)
        setComponentsVisible(!isCreatingNew, pnlExistingFunctionApp)
    }

    //endregion Button Group
}
