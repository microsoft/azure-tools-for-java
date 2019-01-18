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

package com.microsoft.intellij.runner.webapp.webappconfig.ui.component.webapp

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.ui.HideableTitledPanel
import com.intellij.ui.border.IdeaTitledBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.intellij.component.AzureAppServicePlanSelector
import com.microsoft.intellij.component.AzureComponent
import com.microsoft.intellij.component.AzureResourceGroupSelector
import com.microsoft.intellij.component.AzureSubscriptionsSelector
import com.microsoft.intellij.component.extension.*
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionListener
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

class AzureWebAppPublishComponent(lifetime: Lifetime,
                                  private val project: Project,
                                  private val model: WebAppPublishModel) :
        JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")),
        AzureComponent {

    companion object {

        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_APP_SERVICE_PLAN = "App Service Plan"
        private const val HEADER_OPERATING_SYSTEM = "Operating System"

        private const val WEB_APP_TABLE_COLUMN_OS = "OS"
        private const val WEB_APP_RUNTIME_MISMATCH_WARNING =
                "Selected Azure WebApp runtime '%s' mismatch with Project .Net Core Framework '%s'"

        private const val DEFAULT_APP_NAME = "webapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-"

        private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)
        private val netAppVersionRegex = Regex("\\.NETFramework,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

        private val indentionSize = JBUI.scale(7)

        private val warningIcon = AllIcons.General.BalloonWarning
    }

    // Web App Selector
    private val pnlWebAppSelector = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 2"))
    private val rdoUseExistingWebApp = JRadioButton("Use Existing Web App")
    private val rdoCreateNewWebApp = JRadioButton("Create New Web App")

    // Existing Web App
    private val pnlExistingWebApp = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    val pnlExistingWebAppTable = WebAppTableComponent()
    private val lblRuntimeMismatchWarning = JLabel()

    // New Web App
    private val pnlCreateWebApp = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val pnlAppName = WebAppNameComponent(lifetime.createNested())

    val pnlSubscription = AzureSubscriptionsSelector()

    private val pnlResourceGroup = AzureResourceGroupSelector(lifetime.createNested())
    private val pnlResourceGroupHolder = HideableTitledPanel(HEADER_RESOURCE_GROUP, pnlResourceGroup, true)

    private val pnlOperatingSystem = OperatingSystemSelector()
    private val pnlOperatingSystemHolder = HideableTitledPanel(HEADER_OPERATING_SYSTEM, pnlOperatingSystem, true)

    private val pnlAppServicePlan = AzureAppServicePlanSelector(lifetime.createNested())
    private val pnlAppServicePlanHolder = HideableTitledPanel(HEADER_APP_SERVICE_PLAN, pnlAppServicePlan, true)

    // Project
    private val pnlProject = PublishableProjectSelector(project)

    // Web App Settings
    private val pnlWebAppPublishSettings = WebAppAfterPublishSettingPanel()

    init {
        initWebAppSelector()
        initExistingWebAppPanel()
        initRuntimeMismatchWarningLabel()
        initCreateWebAppPanel()
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

        pnlAppName.txtAppName.text =
                if (config.webAppName.isEmpty()) "$DEFAULT_APP_NAME$dateString"
                else config.webAppName

        pnlAppServicePlan.txtAppServicePlanName.text =
                if (config.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString"
                else config.appServicePlanName

        pnlResourceGroup.txtResourceGroupName.text = "$DEFAULT_RESOURCE_GROUP_NAME$dateString"

        if (config.isCreatingWebApp) rdoCreateNewWebApp.doClick()
        else rdoUseExistingWebApp.doClick()

        if (config.isCreatingResourceGroup) pnlResourceGroup.rdoCreateResourceGroup.doClick()
        else pnlResourceGroup.rdoExistingResourceGroup.doClick()

        if (config.isCreatingAppServicePlan) pnlAppServicePlan.rdoCreateAppServicePlan.doClick()
        else pnlAppServicePlan.rdoExistingAppServicePlan.doClick()

        if (config.operatingSystem == OperatingSystem.WINDOWS) pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
        else pnlOperatingSystem.rdoOperatingSystemLinux.doClick()

        val isOpenInBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.openInBrowserDefaultValue)

        if (isOpenInBrowser)
            pnlWebAppPublishSettings.checkBoxOpenInBrowserAfterPublish.doClick()

        pnlExistingWebAppTable.btnRefresh.isEnabled = false
    }

    fun applyConfig(model: WebAppPublishModel) {
        model.subscription = pnlSubscription.cbSubscription.getSelectedValue()
        model.publishableProject = pnlProject.lastSelectedProject

        model.isCreatingWebApp = rdoCreateNewWebApp.isSelected
        if (rdoCreateNewWebApp.isSelected) {
            model.webAppName = pnlAppName.txtAppName.text

            model.isCreatingResourceGroup = pnlResourceGroup.rdoCreateResourceGroup.isSelected
            if (pnlResourceGroup.rdoCreateResourceGroup.isSelected) {
                model.resourceGroupName = pnlResourceGroup.txtResourceGroupName.text
            } else {
                model.resourceGroupName = pnlResourceGroup.lastSelectedResourceGroup?.name() ?: ""
            }

            if (pnlOperatingSystem.isWindows) {
                model.operatingSystem = OperatingSystem.WINDOWS
                // Set .Net Framework version based on project config
                val publishableProject = model.publishableProject
                if (publishableProject != null && !publishableProject.isDotNetCore) {
                    model.netFrameworkVersion =
                            if (getProjectNetFrameworkVersion(publishableProject).startsWith("4")) NetFrameworkVersion.fromString("4.7")
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

            model.isCreatingAppServicePlan = pnlAppServicePlan.rdoCreateAppServicePlan.isSelected
            if (pnlAppServicePlan.rdoCreateAppServicePlan.isSelected) {
                model.appServicePlanName = pnlAppServicePlan.txtAppServicePlanName.text
                model.location = pnlAppServicePlan.cbLocation.getSelectedValue()?.region() ?: model.location
                model.pricingTier = pnlAppServicePlan.cbPricingTier.getSelectedValue() ?: model.pricingTier
            } else {
                model.appServicePlanId = pnlAppServicePlan.cbAppServicePlan.getSelectedValue()?.id() ?: model.appServicePlanId
            }
        } else {
            val selectedWebApp = pnlExistingWebAppTable.lastSelectedWebApp
            model.webAppId = selectedWebApp?.id() ?: ""
            model.operatingSystem = selectedWebApp?.operatingSystem() ?: OperatingSystem.WINDOWS
            if (model.operatingSystem == OperatingSystem.LINUX) {
                val dotNetCoreVersionArray = selectedWebApp?.linuxFxVersion()?.split('|')
                val netCoreRuntime =
                        if (dotNetCoreVersionArray != null && dotNetCoreVersionArray.size == 2) RuntimeStack(dotNetCoreVersionArray[0], dotNetCoreVersionArray[1])
                        else WebAppPublishModel.defaultRuntime
                model.netCoreRuntime = netCoreRuntime
            }
        }
    }

    override fun initComponentValidation() {
        pnlAppName.initComponentValidation()
        pnlSubscription.initComponentValidation()
        pnlResourceGroup.initComponentValidation()
        pnlAppServicePlan.initComponentValidation()
        pnlProject.initComponentValidation()
    }

    //region Web App Selector

    private fun initWebAppSelector() {
        pnlWebAppSelector.apply {
            add(rdoUseExistingWebApp)
            add(rdoCreateNewWebApp, "gapbefore $indentionSize")
        }
    }

    //endregion Web App Selector

    //region Existing Web App

    private fun initExistingWebAppPanel() {
        initWebAppTable()
        initRefreshButton()

        pnlExistingWebApp.apply {
            border = IdeaTitledBorder("Choose Web App", 0, JBUI.emptyInsets())
            add(pnlExistingWebAppTable, "growx")
            add(lblRuntimeMismatchWarning, "growx")
        }
    }

    private fun initWebAppTable()  {
        pnlExistingWebAppTable.tableSelectAction = selectionAction@{ webApp ->
            val publishableProject = pnlProject.cbProject.getSelectedValue() ?: return@selectionAction
            checkSelectedProjectAgainstWebAppRuntime(webApp, publishableProject)
        }
    }

    private fun initRefreshButton() {
        val action = pnlExistingWebAppTable.tableRefreshAction

        pnlExistingWebAppTable.tableRefreshAction = {
            setRuntimeMismatchWarning(false)
            action()
        }
    }

    private fun initRuntimeMismatchWarningLabel() {
        lblRuntimeMismatchWarning.icon = warningIcon
        setRuntimeMismatchWarning(false)
    }

    //endregion Existing Web App

    //region Create Web App

    private fun initCreateWebAppPanel() {
        pnlCreateWebApp.apply {
            add(pnlAppName, "growx")
            add(pnlSubscription, "growx")
            add(pnlResourceGroupHolder, "growx")
            add(pnlOperatingSystemHolder, "growx")
            add(pnlAppServicePlanHolder, "growx")
        }
    }

    private fun initProjectPanel() {
        pnlProject.listenerAction = { publishableProject ->
            setOperatingSystemRadioButtons(publishableProject)
            filterWebAppTableContent(publishableProject)

            val webApp = pnlExistingWebAppTable.lastSelectedWebApp
            if (webApp != null)
                checkSelectedProjectAgainstWebAppRuntime(webApp, publishableProject)
        }
    }

    private fun setOperatingSystemRadioButtons(publishableProject: PublishableProjectModel) {
        setComponentsEnabled(publishableProject.isDotNetCore, pnlOperatingSystem.rdoOperatingSystemLinux)

        if (!publishableProject.isDotNetCore) {
            pnlOperatingSystem.rdoOperatingSystemWindows.doClick()
            toggleOperatingSystem(OperatingSystem.WINDOWS)
        }
    }

    private fun checkSelectedProjectAgainstWebAppRuntime(webApp: WebApp, publishableProject: PublishableProjectModel) {
        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            setRuntimeMismatchWarning(false)
            return
        }

        // DOTNETCORE|2.0 -> 2.0
        val webAppFrameworkVersion = webApp.linuxFxVersion().split('|').getOrNull(1)

        // .NETCoreApp,Version=v2.0 -> 2.0
        val projectNetCoreVersion = getProjectNetCoreFrameworkVersion(publishableProject)
        setRuntimeMismatchWarning(
                webAppFrameworkVersion != projectNetCoreVersion,
                String.format(WEB_APP_RUNTIME_MISMATCH_WARNING, webAppFrameworkVersion, projectNetCoreVersion))
    }

    private fun setRuntimeMismatchWarning(show: Boolean, message: String = "") {
        lblRuntimeMismatchWarning.text = message
        lblRuntimeMismatchWarning.isVisible = show
    }

    //endregion Create Web App

    //region Button Group

    private fun initButtonGroupsState() {
        initWebAppButtonGroup()
        initResourceGroupButtonGroup()
        initOperatingSystemButtonGroup()
        initAppServicePlanButtonGroup()
    }

    private fun initWebAppButtonGroup() {
        initButtonsGroup(hashMapOf(
                rdoUseExistingWebApp to ActionListener { toggleWebAppPanel(false) },
                rdoCreateNewWebApp to ActionListener { toggleWebAppPanel(true) }))
        toggleWebAppPanel(model.isCreatingWebApp)
    }

    private fun initResourceGroupButtonGroup() {
        pnlResourceGroup.toggleResourceGroupPanel(model.isCreatingResourceGroup)
    }

    private fun initOperatingSystemButtonGroup() {
        pnlOperatingSystem.rdoOperatingSystemWindows.addActionListener { toggleOperatingSystem(OperatingSystem.WINDOWS) }
        pnlOperatingSystem.rdoOperatingSystemLinux.addActionListener { toggleOperatingSystem(OperatingSystem.LINUX) }

        toggleOperatingSystem(model.operatingSystem)
    }

    private fun initAppServicePlanButtonGroup() {
        pnlAppServicePlan.toggleAppServicePlanPanel(model.isCreatingAppServicePlan)
    }

    private fun toggleWebAppPanel(isCreatingNew: Boolean) {
        setComponentsVisible(isCreatingNew, pnlCreateWebApp)
        setComponentsVisible(!isCreatingNew, pnlExistingWebApp)
    }

    private fun toggleOperatingSystem(operatingSystem: OperatingSystem) {
        pnlAppServicePlan.cbAppServicePlan.fillComboBox(
                filterAppServicePlans(operatingSystem, pnlAppServicePlan.cachedAppServicePlan),
                pnlAppServicePlan.lastSelectedAppServicePlan
        )

        pnlAppServicePlan.cbPricingTier.fillComboBox(
                filterPricingTiers(operatingSystem, pnlAppServicePlan.cachedPricingTier),
                pnlAppServicePlan.lastSelectedAppServicePlan?.pricingTier()
        )
    }

    //endregion Button Group

    //region Filtering

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(operatingSystem: OperatingSystem,
                                      appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans
                .filter { it.operatingSystem() == operatingSystem }
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
    }

    /**
     * Filter Pricing Tier based on Operating System
     *
     * Note: We should hide "Free" and "Shared" Pricing Tiers for Linux OS
     */
    private fun filterPricingTiers(operatingSystem: OperatingSystem,
                                   prices: List<PricingTier>): List<PricingTier> {
        if (operatingSystem == OperatingSystem.WINDOWS) return prices
        return prices.filter { it != PricingTier.FREE_F1 && it != PricingTier.SHARED_D1 }
    }

    /**
     * Filter Web App table content based on selected project.
     * Here we should filter all Linux instances for selected project with full .Net Target Framework
     *
     * @param publishableProject a selected project
     */
    private fun filterWebAppTableContent(publishableProject: PublishableProjectModel) {
        val sorter = pnlExistingWebAppTable.table.rowSorter as? TableRowSorter<*> ?: return

        if (publishableProject.isDotNetCore) {
            sorter.rowFilter = null
            return
        }

        val osColumnIndex = (pnlExistingWebAppTable.table.model as DefaultTableModel).findColumn(WEB_APP_TABLE_COLUMN_OS)
        require(osColumnIndex >= 0) { "Column index is out of range" }
        sorter.rowFilter = javax.swing.RowFilter.regexFilter(OperatingSystem.WINDOWS.name.toLowerCase().capitalize(), osColumnIndex)
    }

    //endregion Filtering

    //region Fill Values

    fun fillSubscription(subscriptions: List<Subscription>) {
        pnlSubscription.fillSubscriptionComboBox(subscriptions, model.subscription)
    }

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        pnlResourceGroup.fillResourceGroupComboBox(resourceGroups) {
            resourceGroup -> resourceGroup.name() == model.resourceGroupName
        }
    }

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {
        pnlAppServicePlan.fillAppServicePlanComboBox(
                filterAppServicePlans(pnlOperatingSystem.deployOperatingSystem, appServicePlans)) {
            appServicePlan -> appServicePlan.id() == model.appServicePlanId
        }
    }

    fun fillLocation(locations: List<Location>) {
        pnlAppServicePlan.fillLocationComboBox(locations, model.location)
    }

    fun fillPricingTier(pricingTiers: List<PricingTier>) {
        pnlAppServicePlan.fillPricingTiler(
                filterPricingTiers(pnlOperatingSystem.deployOperatingSystem, pricingTiers),
                model.pricingTier
        )
    }

    fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlProject.fillProjectComboBox(publishableProjects, model.publishableProject)

        if (model.publishableProject != null && publishableProjects.contains(model.publishableProject!!)) {
            setOperatingSystemRadioButtons(model.publishableProject!!)
            pnlProject.lastSelectedProject = model.publishableProject
        }
    }

    //endregion Fill Values

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

    private fun getCurrentFrameworkId(publishableProject: PublishableProjectModel): String? {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        return targetFramework?.currentTargetFrameworkId?.valueOrNull?.id
    }

    private fun getProjectNetFrameworkVersion(publishableProject: PublishableProjectModel): String {
        val defaultVersion = "4.7"
        val currentFramework = getCurrentFrameworkId(publishableProject) ?: return defaultVersion
        return netAppVersionRegex.find(currentFramework)?.groups?.get(1)?.value ?: defaultVersion
    }
}
