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

package com.microsoft.intellij.runner.webapp.config.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import com.microsoft.intellij.ui.component.AzureComponent
import com.microsoft.intellij.ui.component.ExistingOrNewSelector
import com.microsoft.intellij.ui.component.PublishableProjectComponent
import com.microsoft.intellij.ui.component.appservice.AppAfterPublishSettingPanel
import com.microsoft.intellij.ui.component.appservice.WebAppExistingComponent
import com.microsoft.intellij.ui.extension.getSelectedValue
import com.microsoft.intellij.ui.extension.setComponentsVisible
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import javax.swing.JPanel

class WebAppPublishComponent(private val lifetime: Lifetime,
                             private val project: Project,
                             private val model: WebAppPublishModel) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {
        private const val DEFAULT_APP_NAME = "webapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"

        private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
        private val netAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
    }

    private val pnlWebAppSelector = ExistingOrNewSelector(message("run_config.publish.form.web_app.existing_new_selector"))
    val pnlExistingWebApp = WebAppExistingComponent(lifetime = lifetime.createNested())
    val pnlCreateWebApp = WebAppCreateNewComponent(lifetime.createNested())
    private val pnlProject = PublishableProjectComponent(project)
    private val pnlWebAppPublishSettings = AppAfterPublishSettingPanel()

    init {
        initExistingWebAppPanel()
        initProjectPanel()

        add(pnlWebAppSelector)
        add(pnlExistingWebApp, "growx")
        add(pnlCreateWebApp, "growx")
        add(pnlProject, "growx")
        add(pnlWebAppPublishSettings, "growx")

        initButtonGroupsState()

        initComponentValidation()
    }

    fun resetFromConfig(config: WebAppPublishModel, dateString: String) {
        if (config.publishableProject != null)
            pnlProject.cbProject.selectedItem = config.publishableProject

        pnlCreateWebApp.pnlAppName.txtAppName.text =
                if (config.appName.isEmpty()) "$DEFAULT_APP_NAME$dateString"
                else config.appName

        pnlCreateWebApp.pnlAppServicePlan.txtName.text =
                if (config.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString"
                else config.appServicePlanName

        pnlCreateWebApp.pnlResourceGroup.txtResourceGroupName.text = "$DEFAULT_RESOURCE_GROUP_NAME$dateString"

        // Web App
        if (config.isCreatingNewApp) pnlWebAppSelector.rdoCreateNew.doClick()
        else pnlWebAppSelector.rdoUseExisting.doClick()

        // Resource Group
        if (config.isCreatingResourceGroup) pnlCreateWebApp.pnlResourceGroup.rdoCreateNew.doClick()
        else pnlCreateWebApp.pnlResourceGroup.rdoUseExisting.doClick()

        // App Service Plan
        if (config.isCreatingAppServicePlan) pnlCreateWebApp.pnlAppServicePlan.rdoCreateNew.doClick()
        else pnlCreateWebApp.pnlAppServicePlan.rdoUseExisting.doClick()

        // Operating System
        if (config.operatingSystem == OperatingSystem.WINDOWS) pnlCreateWebApp.pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
        else pnlCreateWebApp.pnlOperatingSystem.rdoOperatingSystemLinux.doClick()

        // Deployment Slot
        pnlExistingWebApp.pnlDeploymentSlotSettings.loadFinished.adviseOnce(lifetime.createNested()) { slotsData ->
            val panel = pnlExistingWebApp.pnlDeploymentSlotSettings
            if (config.isDeployToSlot.xor(panel.checkBoxIsEnabled.isSelected)) {
                panel.checkBoxIsEnabled.doClick()

                if (config.appId.isNotEmpty() && config.appId == slotsData.appId && config.slotName.isNotEmpty() && slotsData.slots.isNotEmpty()) {
                    val slotToSelect = slotsData.slots.find { it.name() == config.slotName }
                    if (slotToSelect != null)
                        panel.cbDeploymentSlots.selectedItem = slotToSelect
                }
            }
        }

        // Settings
        val isOpenInBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        if (isOpenInBrowser)
            pnlWebAppPublishSettings.checkBoxOpenInBrowserAfterPublish.doClick()

        pnlExistingWebApp.pnlExistingAppTable.btnRefresh.isEnabled = false
    }

    fun applyConfig(model: WebAppPublishModel) {
        model.subscription = pnlCreateWebApp.pnlSubscription.cbSubscription.getSelectedValue()
        model.publishableProject = pnlProject.lastSelectedProject

        model.isCreatingNewApp = pnlWebAppSelector.isCreateNew

        model.appName = pnlCreateWebApp.pnlAppName.appName

        model.isCreatingResourceGroup = pnlCreateWebApp.pnlResourceGroup.isCreateNew
        if (pnlCreateWebApp.pnlResourceGroup.isCreateNew) {
            model.resourceGroupName = pnlCreateWebApp.pnlResourceGroup.resourceGroupName
        } else {
            model.resourceGroupName = pnlCreateWebApp.pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
        }

        if (pnlCreateWebApp.pnlOperatingSystem.isWindows) {
            model.operatingSystem = OperatingSystem.WINDOWS
            // Set .Net Framework version based on project config
            val publishableProject = model.publishableProject
            if (publishableProject != null && !publishableProject.isDotNetCore) {
                model.netFrameworkVersion =
                        if (getProjectNetFrameworkVersion(publishableProject).startsWith("4"))
                            NetFrameworkVersion.fromString("4.7")
                        else NetFrameworkVersion.fromString("3.5")
            }
        } else {
            model.operatingSystem = OperatingSystem.LINUX
            // Set Net Core runtime based on project config
            val publishableProject = pnlProject.lastSelectedProject
            if (publishableProject != null && publishableProject.isDotNetCore) {
                val netCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
                model.netCoreRuntime = RuntimeStack("DOTNETCORE", netCoreVersion)
            }
        }

        model.isCreatingAppServicePlan = pnlCreateWebApp.pnlAppServicePlan.rdoCreateNew.isSelected
        model.appServicePlanName = pnlCreateWebApp.pnlAppServicePlan.servicePlanName
        model.location = pnlCreateWebApp.pnlAppServicePlan.cbLocation.getSelectedValue()?.region() ?: model.location
        model.pricingTier = pnlCreateWebApp.pnlAppServicePlan.cbPricingTier.getSelectedValue() ?: model.pricingTier
        if (!model.isCreatingAppServicePlan)
            model.appServicePlanId = pnlCreateWebApp.pnlAppServicePlan.cbAppServicePlan.getSelectedValue()?.id()
                    ?: model.appServicePlanId

        val selectedResource = pnlExistingWebApp.pnlExistingAppTable.lastSelectedResource
        val selectedWebApp = selectedResource?.resource
        model.appId = selectedWebApp?.id() ?: ""

        if (!model.isCreatingNewApp) {
            model.subscription = AzureMvpModel.getInstance()
                    .selectedSubscriptions
                    .find { it.subscriptionId() == selectedResource?.subscriptionId }
        }

        val operatingSystem = selectedWebApp?.operatingSystem() ?: OperatingSystem.WINDOWS
        if (operatingSystem == OperatingSystem.LINUX) {
            val dotNetCoreVersionArray = selectedWebApp?.linuxFxVersion()?.split('|')
            val netCoreRuntime =
                    if (dotNetCoreVersionArray != null && dotNetCoreVersionArray.size == 2)
                        RuntimeStack(dotNetCoreVersionArray[0], dotNetCoreVersionArray[1])
                    else WebAppPublishModel.defaultRuntime
            model.netCoreRuntime = netCoreRuntime
        }

        val slotSettings = pnlExistingWebApp.pnlDeploymentSlotSettings
        model.isDeployToSlot = slotSettings.checkBoxIsEnabled.isSelected
        model.slotName = slotSettings.cbDeploymentSlots.getSelectedValue()?.name() ?: model.slotName
    }

    //region Fill Values

    fun fillAppsTable(webApps: List<ResourceEx<WebApp>>) =
            pnlExistingWebApp.fillAppsTable(webApps, model.appId)

    fun fillSubscription(subscriptions: List<Subscription>) =
            pnlCreateWebApp.fillSubscription(subscriptions, model.subscription)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) =
            pnlCreateWebApp.fillResourceGroup(resourceGroups, model.resourceGroupName)

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) =
            pnlCreateWebApp.fillAppServicePlan(appServicePlans, model.appServicePlanId)

    fun fillLocation(locations: List<Location>) =
            pnlCreateWebApp.fillLocation(locations, model.location)

    fun fillPricingTier(pricingTiers: List<PricingTier>) =
            pnlCreateWebApp.fillPricingTier(pricingTiers, model.pricingTier)

    fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlProject.fillProjectComboBox(publishableProjects, model.publishableProject)

        if (model.publishableProject != null && publishableProjects.contains(model.publishableProject!!)) {
            pnlCreateWebApp.setOperatingSystemRadioButtons(model.publishableProject!!.isDotNetCore)
            pnlProject.lastSelectedProject = model.publishableProject
        }
    }

    //endregion Fill Values

    //region Existing Web App

    private fun initExistingWebAppPanel() {
        initWebAppTable()
    }

    private fun initWebAppTable()  {
        pnlExistingWebApp.pnlExistingAppTable.table.selectionModel.addListSelectionListener {
            val publishableProject = pnlProject.cbProject.getSelectedValue() ?: return@addListSelectionListener
            val app = pnlExistingWebApp.pnlExistingAppTable.getTableSelectedApp() ?: return@addListSelectionListener
            checkSelectedProjectAgainstWebAppRuntime(app, publishableProject)
        }
    }

    //endregion Existing Web App

    //region Project

    private fun initProjectPanel() {

        pnlProject.canBePublishedAction = { publishableProject -> canBePublishedToAzure(publishableProject) }

        pnlProject.listenerAction = { publishableProject ->
            pnlCreateWebApp.setOperatingSystemRadioButtons(publishableProject.isDotNetCore)
            pnlExistingWebApp.filterAppTableContent(publishableProject.isDotNetCore)

            val webApp = pnlExistingWebApp.pnlExistingAppTable.lastSelectedResource?.resource
            if (webApp != null)
                checkSelectedProjectAgainstWebAppRuntime(webApp, publishableProject)
        }
    }

    private fun checkSelectedProjectAgainstWebAppRuntime(webApp: WebApp, publishableProject: PublishableProjectModel) {
        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            pnlExistingWebApp.setRuntimeMismatchWarning(false)
            return
        }

        // DOTNETCORE|2.0 -> 2.0
        val webAppFrameworkVersion = webApp.linuxFxVersion().split('|').getOrNull(1)

        // .NETCoreApp,Version=v2.0 -> 2.0
        val projectNetCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
        pnlExistingWebApp.setRuntimeMismatchWarning(
                webAppFrameworkVersion != projectNetCoreVersion,
                message("run_config.publish.form.web_app.runtime_mismatch_warning", webAppFrameworkVersion.toString(), projectNetCoreVersion)
        )
    }

    //endregion Project

    //region Button Group

    private fun initButtonGroupsState() {
        initWebAppButtonGroup()
        initResourceGroupButtonGroup()
        initOperatingSystemButtonGroup()
        initAppServicePlanButtonGroup()
    }

    private fun initWebAppButtonGroup() {
        pnlWebAppSelector.rdoUseExisting.addActionListener { toggleWebAppPanel(false) }
        pnlWebAppSelector.rdoCreateNew.addActionListener { toggleWebAppPanel(true) }
        toggleWebAppPanel(model.isCreatingNewApp)
    }

    private fun toggleWebAppPanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateWebApp)
        setComponentsVisible(!isCreatingNew, pnlExistingWebApp)
    }

    private fun initResourceGroupButtonGroup() {
        pnlCreateWebApp.pnlResourceGroup.toggleResourceGroupPanel(model.isCreatingResourceGroup)
    }

    private fun initOperatingSystemButtonGroup() {
        pnlCreateWebApp.toggleOperatingSystem(model.operatingSystem)
    }

    private fun initAppServicePlanButtonGroup() {
        pnlCreateWebApp.pnlAppServicePlan.toggleAppServicePlanPanel(model.isCreatingAppServicePlan)
    }

    //endregion Button Group

    /**
     * Get a Target .Net Core Framework value for a publishable .net core project
     *
     * @param publishableProject a publishable project instance
     * @return [String] a version for a Target Framework in format:
     *                  for a target framework ".NETCoreApp,Version=v2.0", the method returns "2.0"
     */
    private fun getProjectNetCoreFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "2.1"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netCoreAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }

    private fun getProjectNetFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "4.7"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }

    private fun getCurrentFrameworkId(publishableProject: PublishableProjectModel): String? {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        return targetFramework?.currentTargetFrameworkId?.valueOrNull?.framework?.id
    }

    private fun canBePublishedToAzure(publishableProject: PublishableProjectModel) =
            publishableProject.isWeb && (publishableProject.isDotNetCore || SystemInfo.isWindows)
}
