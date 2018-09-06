package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.AnActionButton
import com.intellij.ui.HideableDecorator
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.nodes.isProject
import com.jetbrains.rider.projectView.nodes.isUnloadedProject
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.lifetime
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * The setting panel for web app deployment run configuration.
 * TODO: refactor this, almost 1k lines as of 2018-09-06
 */
class RiderWebAppSettingPanel(project: Project,
                              private val configuration: RiderWebAppConfiguration)
    : AzureRiderSettingPanel<RiderWebAppConfiguration>(project), DotNetWebAppDeployMvpView {

    companion object {

        // const
        private const val WEB_APP_SETTINGS_PANEL_NAME = "Run On Web App"

        private const val RUN_CONFIG_PREFIX = "Azure Web App"

        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_APP_SERVICE_PLAN = "App Service Plan"
        private const val HEADER_OPERATING_SYSTEM = "Operating System"
        private const val HEADER_RUNTIME = "Runtime"

        private const val WEB_APP_TABLE_COLUMN_SUBSCRIPTION = "Subscription"
        private const val WEB_APP_TABLE_COLUMN_NAME = "Name"
        private const val WEB_APP_TABLE_COLUMN_RESOURCE_GROUP = "Resource group"
        private const val WEB_APP_TABLE_COLUMN_LOCATION = "Location"
        private const val WEB_APP_TABLE_COLUMN_OS = "OS"
        private const val WEB_APP_TABLE_COLUMN_DOTNET_VERSION = ".Net Version"

        private const val BUTTON_REFRESH_NAME = "Refresh"

        private const val NOT_APPLICABLE = "N/A"
        private const val TABLE_LOADING_MESSAGE = "Loading ... "
        private const val TABLE_EMPTY_MESSAGE = "No available Web Apps."
        private const val PROJECTS_EMPTY_MESSAGE = "No projects to publish"

        private const val DEFAULT_APP_NAME = "webapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RGP_NAME = "rg-webapp-"
    }

    // presenter
    private val myView = DotNetWebAppDeployViewPresenter<RiderWebAppSettingPanel>()

    // cache variable
    private var selectedWebApp: ResourceEx<WebApp>? = null
    private var cachedWebAppList = listOf<ResourceEx<WebApp>>()

    private var lastSelectedSubscriptionId: String = ""

    private var lastSelectedResourceGroupName: String = ""

    private var lastSelectedOperatingSystem: OperatingSystem = AzureDotNetWebAppSettingModel.defaultOperatingSystem

    private var cachedAppServicePlan = listOf<AppServicePlan>()
    private var lastSelectedLocation: String = ""
    private var cachedPricingTier = listOf<PricingTier>()
    private var lastSelectedPriceTier: PricingTier = AzureDotNetWebAppSettingModel.defaultPricingTier

    private var lastSelectedRuntime: RuntimeStack = AzureDotNetWebAppSettingModel.defaultRuntime

    // Panels
    override var mainPanel: JPanel = pnlRoot
    private lateinit var pnlRoot: JPanel

    // Existing Web App
    private lateinit var rdoUseExistingWebApp: JRadioButton
    private lateinit var pnlExistingWebApp: JPanel

    private lateinit var table: JBTable
    private lateinit var btnRefresh: AnActionButton

    // New Web App
    private lateinit var rdoCreateNewWebApp: JRadioButton
    private lateinit var pnlCreateWebApp: JPanel

    private lateinit var pnlWebAppTable: JPanel
    private lateinit var txtSelectedWebApp: JTextField

    private lateinit var txtWebAppName: JTextField
    private lateinit var cbSubscription: JComboBox<Subscription>

    // Resource Group
    private lateinit var pnlResourceGroupHolder: JPanel
    private lateinit var pnlResourceGroup: JPanel

    private lateinit var rdoUseExistResGrp: JRadioButton
    private lateinit var cbResourceGroup: JComboBox<ResourceGroup>

    private lateinit var rdoCreateResGrp: JRadioButton
    private lateinit var txtResourceGroupName: JTextField

    // Operating System
    private lateinit var pnlOperatingSystemHolder: JPanel
    private lateinit var pnlOperatingSystem: JPanel

    private lateinit var cbOperatingSystem: JComboBox<OperatingSystem>

    // App Service Plan
    private lateinit var pnlAppServicePlanHolder: JPanel
    private lateinit var pnlAppServicePlan: JPanel

    private lateinit var rdoCreateAppServicePlan: JRadioButton
    private lateinit var txtAppServicePlanName: JTextField
    private lateinit var cbLocation: JComboBox<Location>
    private lateinit var cbPricingTier: JComboBox<PricingTier>

    private lateinit var rdoUseExistAppServicePlan: JRadioButton
    private lateinit var cbAppServicePlan: JComboBox<AppServicePlan>
    private lateinit var lblLocation: JLabel
    private lateinit var lblPricingTier: JLabel

    // Runtime
    private lateinit var pnlRuntimeHolder: JPanel
    private lateinit var pnlRuntime: JPanel
    private lateinit var cbRuntime: JComboBox<RuntimeStack>

    // Project
    private lateinit var pnlProject: JPanel
    private lateinit var cbProject: JComboBox<PublishableProjectModel>

    override val panelName: String
        get() = WEB_APP_SETTINGS_PANEL_NAME

    init {
        myView.onAttachView(this)

        updateAzureModelInBackground()

        initButtonGroupsState()
        setUIComponents(project)
    }

    //region Read From Config

    /**
     * Reset all controls from configuration.
     * Function is triggered while constructing the panel.
     *
     * @param configuration - Web App Configuration instance
     */
    public override fun resetFromConfig(configuration: RiderWebAppConfiguration) {
        val dateString = SimpleDateFormat("yyMMddHHmmss").format(Date())

        val model = configuration.model

        txtWebAppName.text = if (model.webAppName.isEmpty()) "$DEFAULT_APP_NAME$dateString" else model.webAppName
        txtAppServicePlanName.text = if (model.appServicePlanName.isEmpty()) "$DEFAULT_PLAN_NAME$dateString" else model.appServicePlanName
        txtResourceGroupName.text = if (model.resourceGroupName.isEmpty()) "$DEFAULT_RGP_NAME$dateString" else model.resourceGroupName

        if (model.isCreatingWebApp) {
            rdoCreateNewWebApp.doClick()
            if (model.isCreatingResourceGroup) {
                rdoCreateResGrp.doClick()
            } else {
                rdoUseExistResGrp.doClick()
            }
            if (model.isCreatingAppServicePlan) {
                rdoCreateAppServicePlan.doClick()
            } else {
                rdoUseExistAppServicePlan.doClick()
            }
        } else {
            rdoUseExistingWebApp.doClick()
        }
        btnRefresh.isEnabled = false

        myView.onLoadSubscription()
        myView.onLoadWebApps()
        myView.onLoadPricingTier()
        myView.onLoadOperatingSystem()
        myView.onLoadRuntime()
    }

    /**
     * Function is triggered by any content change events.
     *
     * @param configuration configuration instance
     */
    override fun apply(configuration: RiderWebAppConfiguration) {

        val model = configuration.model

        val publishableProject = cbProject.getItemAt(cbProject.selectedIndex)
        model.publishableProject = publishableProject

        if (rdoUseExistingWebApp.isSelected) {
            model.isCreatingWebApp = false
            model.subscriptionId = selectedWebApp?.subscriptionId ?: ""
            model.webAppId = selectedWebApp?.resource?.id() ?: ""
            model.operatingSystem = selectedWebApp?.resource?.operatingSystem() ?: OperatingSystem.WINDOWS
            if (model.operatingSystem == OperatingSystem.LINUX) {
                val dotNetCoreVersionArray = selectedWebApp?.resource?.linuxFxVersion()?.split('|')
                val runtime =
                        if (dotNetCoreVersionArray != null && dotNetCoreVersionArray.size == 2) RuntimeStack(dotNetCoreVersionArray[0], dotNetCoreVersionArray[1])
                        else AzureDotNetWebAppSettingModel.defaultRuntime
                // TODO: Add a warning for a user when publishing to an Linux instance with runtime that mismatch with
                // TODO: required for a publishable project
                model.runtime = runtime
            }
        } else if (rdoCreateNewWebApp.isSelected) {
            model.isCreatingWebApp = true

            model.webAppName = txtWebAppName.text
            model.subscriptionId = lastSelectedSubscriptionId

            // Resource group
            if (rdoCreateResGrp.isSelected) {
                model.isCreatingResourceGroup = true
                model.resourceGroupName = txtResourceGroupName.text
            } else {
                model.isCreatingResourceGroup = false
                model.resourceGroupName = lastSelectedResourceGroupName
            }

            // App service plan
            if (rdoCreateAppServicePlan.isSelected) {
                model.isCreatingAppServicePlan = true
                model.appServicePlanName = txtAppServicePlanName.text
                model.location = lastSelectedLocation
                model.pricingTier = lastSelectedPriceTier
            } else {
                model.isCreatingAppServicePlan = false
                val appServicePlan = cbAppServicePlan.getItemAt(cbAppServicePlan.selectedIndex)
                if (appServicePlan != null) {
                    model.appServicePlanId = appServicePlan.id()
                }
            }

            model.operatingSystem = lastSelectedOperatingSystem

            // runtime only makes sense for linux
            if (model.operatingSystem == OperatingSystem.WINDOWS) model.runtime = AzureDotNetWebAppSettingModel.defaultRuntime
            else model.runtime = lastSelectedRuntime
        }
    }

    //endregion Read From Config

    //region MVP View

    /**
     * Render table with existing Web apps to publish
     *
     * @param webAppLists - list of web apps to render
     */
    override fun renderWebAppsTable(webAppLists: List<ResourceEx<WebApp>>) {
        btnRefresh.isEnabled = true
        table.emptyText.text = TABLE_EMPTY_MESSAGE

        val sortedWebApps = webAppLists.sortedWith(compareBy (
                { it.resource.operatingSystem() },
                { it.subscriptionId },
                { it.resource.resourceGroupName() }))

        cachedWebAppList = sortedWebApps

        setWebAppTableContent(sortedWebApps)
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        cbSubscription.removeAllItems()
        if (subscriptions.isEmpty()) {
            lastSelectedSubscriptionId = ""
            return
        }

        subscriptions.forEach {
            cbSubscription.addItem(it)
            if (it.subscriptionId() == configuration.subscriptionId) {
                cbSubscription.selectedItem = it
            }
        }
    }

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        cbResourceGroup.removeAllItems()
        if (resourceGroups.isEmpty()) {
            lastSelectedResourceGroupName = ""
            return
        }

        resourceGroups.sortedWith(compareBy { it.name() })
                .forEach {
                    cbResourceGroup.addItem(it)
                    if (it.name() == configuration.model.resourceGroupName) {
                        cbResourceGroup.selectedItem = it
                    }
                }
    }

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {

        cachedAppServicePlan = appServicePlans

        if (appServicePlans.isEmpty()) {
            lblLocation.text = NOT_APPLICABLE
            lblPricingTier.text = NOT_APPLICABLE
            return
        }

        val filteredPlans = filterAppServicePlans(lastSelectedOperatingSystem, appServicePlans)
        setAppServicePlanContent(filteredPlans)
    }

    override fun fillOperatingSystem(operatingSystems: List<OperatingSystem>) {
        cbOperatingSystem.removeAllItems()
        operatingSystems.forEach {
            cbOperatingSystem.addItem(it)
            if (it == configuration.model.operatingSystem) {
                cbOperatingSystem.selectedItem = it
            }
        }
    }

    override fun fillLocation(locations: List<Location>) {
        cbLocation.removeAllItems()
        if (locations.isEmpty()) {
            lastSelectedLocation = ""
            return
        }
        locations.sortedWith(compareBy { it.displayName() })
                .forEach { location ->
                    cbLocation.addItem(location)
                    if (location.name() == configuration.model.location) {
                        cbLocation.selectedItem = location
                    }
                }
    }

    override fun fillPricingTier(prices: List<PricingTier>) {
        cachedPricingTier = prices

        val filteredPrices = filterPricingTiers(lastSelectedOperatingSystem, prices)

        setPricingTierContent(filteredPrices)
    }

    override fun fillRuntime(runtimes: List<RuntimeStack>) {
        cbRuntime.removeAllItems()
        runtimes.forEach {
            cbRuntime.addItem(it)
            if (it == configuration.model.runtime) {
                cbRuntime.selectedItem = it
            }
        }
    }

    private fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        cbProject.removeAllItems()

        filterProjects(publishableProjects)
                publishableProjects.sortedBy { it.projectName }
                .forEach {
                    cbProject.addItem(it)
                    if (it == configuration.model.publishableProject) {
                        cbProject.selectedItem = it
                        toggleProjectComboBox(it.isDotNetCore)
                    }
                }
    }

    /**
     * Set the table rows for web apps table
     *
     * @param webAppLists list of web app to display
     */
    private fun setWebAppTableContent(webAppLists: List<ResourceEx<WebApp>>) {
        if (webAppLists.isEmpty()) return

        val model = table.model as DefaultTableModel
        model.dataVector.clear()

        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager

        for (i in webAppLists.indices) {
            val webApp = webAppLists[i].resource
            val subscription = subscriptionManager.subscriptionIdToSubscriptionMap[webApp.manager().subscriptionId()] ?: continue

            model.addRow(arrayOf(
                    webApp.name(),
                    webApp.resourceGroupName(),
                    webApp.region().label(),
                    webApp.operatingSystem().name.toLowerCase().capitalize(),
                    getNetFrameworkVersion(webApp),
                    subscription.displayName())
            )

            if (webApp.id() == configuration.model.webAppId) {
                table.setRowSelectionInterval(i, i)
            }
        }
    }

    /**
     * Set the App Service Plan Combo Box content
     *
     * @param appServicePlans list of App Service Plans to display
     */
    private fun setAppServicePlanContent(appServicePlans: List<AppServicePlan>) {
        cbAppServicePlan.removeAllItems()

        appServicePlans
                .sortedWith(compareBy({ it.operatingSystem() }, { it.name() }))
                .forEach {
                    cbAppServicePlan.addItem(it)
                    if (it.id() == configuration.model.appServicePlanId) {
                        cbAppServicePlan.selectedItem = it
                    }
                }
    }

    /**
     * Set the Pricing Tier Combo Box content
     *
     * @param pricingTiers list of Pricing Tiers to display
     */
    private fun setPricingTierContent(pricingTiers: List<PricingTier>) {
        cbPricingTier.removeAllItems()

        pricingTiers.forEach {
            cbPricingTier.addItem(it)
            if (it == configuration.model.pricingTier) {
                cbPricingTier.selectedItem = it
            }
        }
    }

    /**
     * Get the valid text for net framework
     *
     * Note: For .NET Framework there are two valid values for version (v4.7 and v3.5). Azure SDK
     *       does not set the right values. They set the "v4.0" for "v4.7" and "v2.0" for "v3.5" instances
     *       For .Net Core Framework we get the correct runtime value from the linux fx version that store
     *       a instance name representation
     *
     * Note: 2.0 and 4.0 are CLR versions most likely
     *
     * @param webApp web app instance
     * @return [String] with a .Net Framework version for a web app instance
     */
    private fun getNetFrameworkVersion(webApp: WebApp): String {
        return if (webApp.operatingSystem() == OperatingSystem.LINUX) {
            "Core v${webApp.linuxFxVersion().split('|')[1]}"
        } else {
            val version = webApp.netFrameworkVersion().toString()
            if (version.startsWith("v4")) "v4.7"
            else "v3.5"
        }
    }

    /**
     * Let the presenter release the view. Will be called by:
     * [com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppSettingEditor.disposeEditor].
     */
    override fun disposeEditor() {
        myView.onDetachView()
    }

    //endregion MVP View

    //region Filters

    /**
     * Filter all non-core apps or non-web apps on full .Net framework
     *
     * @param publishableProjects list of all available publishable projects
     */
    private fun filterProjects(publishableProjects: Collection<PublishableProjectModel>): List<PublishableProjectModel> {
        return publishableProjects.filter { it.isWeb && (it.isDotNetCore || SystemInfo.isWindows) }
    }

    /**
     * Filter App Service Plans to Operating System related values
     */
    private fun filterAppServicePlans(operatingSystem: OperatingSystem,
                                      appServicePlans: List<AppServicePlan>): List<AppServicePlan> {
        return appServicePlans.filter { it.operatingSystem() == operatingSystem }
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

    //endregion Filters

    //region Editing Web App Table

    private fun createUIComponents() {
        initWebAppTable()
    }

    private fun initWebAppTable() {
        initWebAppTableModel()
        initRefreshButton()

        val tableToolbarDecorator = ToolbarDecorator.createDecorator(table)
                .addExtraActions(btnRefresh).setToolbarPosition(ActionToolbarPosition.TOP)

        pnlWebAppTable = tableToolbarDecorator.createPanel()
    }

    private fun initWebAppTableModel() {
        val tableModel = object : DefaultTableModel() {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                return false
            }
        }
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_NAME)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_RESOURCE_GROUP)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_LOCATION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_OS)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_DOTNET_VERSION)
        tableModel.addColumn(WEB_APP_TABLE_COLUMN_SUBSCRIPTION)

        table = JBTable(tableModel)
        table.emptyText.text = TABLE_LOADING_MESSAGE
        table.rowSelectionAllowed = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.selectionModel.addListSelectionListener { event ->
            if (event.valueIsAdjusting) {
                return@addListSelectionListener
            }
            val selectedRow = table.selectedRow
            if (cachedWebAppList.isEmpty() || selectedRow < 0 || selectedRow >= cachedWebAppList.size) {
                selectedWebApp = null
                return@addListSelectionListener
            }
            val webApp = cachedWebAppList[selectedRow]
            selectedWebApp = webApp
            // This is not visible on the UI, but is used to preform a re-validation over selected web app from the table
            txtSelectedWebApp.text = webApp.resource.name()
        }
    }

    private fun initRefreshButton() {
        btnRefresh = object : AnActionButton(BUTTON_REFRESH_NAME, AllIcons.Actions.Refresh) {
            override fun actionPerformed(anActionEvent: AnActionEvent) {
                resetWidget()
                myView.onRefresh()
            }
        }
    }

    private fun resetWidget() {
        btnRefresh.isEnabled = false
        val model = table.model as DefaultTableModel
        model.dataVector.clear()
        model.fireTableDataChanged()
        table.emptyText.text = TABLE_LOADING_MESSAGE
        txtSelectedWebApp.text = ""
    }

    //endregion Editing Web App Table

    //region Button Groups Behavior

    private fun initButtonGroupsState() {
        initWebAppDeployButtonGroup()
        initResourceGroupButtonGroup()
        initAppServicePlanButtonsGroup()
    }

    private fun initWebAppDeployButtonGroup() {
        val btnGrpForDeploy = ButtonGroup()
        btnGrpForDeploy.add(rdoUseExistingWebApp)
        btnGrpForDeploy.add(rdoCreateNewWebApp)
        rdoUseExistingWebApp.addActionListener { toggleDeployPanel(true) }
        rdoCreateNewWebApp.addActionListener { toggleDeployPanel(false) }
        toggleDeployPanel(true)
    }

    private fun initResourceGroupButtonGroup() {
        val btnGrpForResGrp = ButtonGroup()
        btnGrpForResGrp.add(rdoUseExistResGrp)
        btnGrpForResGrp.add(rdoCreateResGrp)
        rdoCreateResGrp.addActionListener { toggleResGrpPanel(true) }
        rdoUseExistResGrp.addActionListener { toggleResGrpPanel(false) }
    }

    private fun initAppServicePlanButtonsGroup() {
        val btnGrpForAppServicePlan = ButtonGroup()
        btnGrpForAppServicePlan.add(rdoUseExistAppServicePlan)
        btnGrpForAppServicePlan.add(rdoCreateAppServicePlan)
        rdoUseExistAppServicePlan.addActionListener { toggleAppServicePlanPanel(false) }
        rdoCreateAppServicePlan.addActionListener { toggleAppServicePlanPanel(true) }
    }

    private fun initProjectsComboBox() {
        project.solution.publishableProjectsModel.publishableProjects.advise(project.lifetime) {
            if (it.newValueOpt != null) {
                fillPublishableProject(project.solution.publishableProjectsModel.publishableProjects.values.toList())
            }
        }
    }

    private fun toggleDeployPanel(isUsingExisting: Boolean) {
        pnlExistingWebApp.isVisible = isUsingExisting
        pnlCreateWebApp.isVisible = !isUsingExisting
    }

    private fun toggleResGrpPanel(isCreatingNew: Boolean) {
        txtResourceGroupName.isEnabled = isCreatingNew
        cbResourceGroup.isEnabled = !isCreatingNew
    }

    private fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        txtAppServicePlanName.isEnabled = isCreatingNew
        cbLocation.isEnabled = isCreatingNew
        cbPricingTier.isEnabled = isCreatingNew
        cbAppServicePlan.isEnabled = !isCreatingNew
        lblLocation.isEnabled = !isCreatingNew
        lblPricingTier.isEnabled = !isCreatingNew
    }

    private fun toggleRuntimePanel(isLinux: Boolean) {
        pnlRuntimeHolder.isVisible = isLinux
    }

    private fun toggleOperatingSystemComboBox(operatingSystem: OperatingSystem) {
        setAppServicePlanContent(filterAppServicePlans(operatingSystem, cachedAppServicePlan))
        setPricingTierContent(filterPricingTiers(operatingSystem, cachedPricingTier))
        toggleRuntimePanel(operatingSystem == OperatingSystem.LINUX)
    }

    private fun toggleProjectComboBox(isDotNetCore: Boolean) {
        cbOperatingSystem.isEnabled = isDotNetCore

        if (isDotNetCore) {
            setWebAppTableContent(cachedWebAppList)
        } else {
            // OS combo box
            cbOperatingSystem.selectedItem = OperatingSystem.WINDOWS

            // Filter table with Windows only web apps
            setWebAppTableContent(
                    cachedWebAppList.filter { it.resource.operatingSystem() == OperatingSystem.WINDOWS })
        }
    }

    //endregion Button Groups Behavior

    //region Configure UI Components

    /**
     * Configure renderer and listeners for all UI Components
     */
    private fun setUIComponents(project: Project) {
        setSubscriptionComboBox()
        setResourceGroupComboBox()
        setOperatingSystemComboBox()

        setAppServicePlanComboBox()
        setLocationComboBox()
        setPricingTierComboBox()

        setRuntimeComboBox()

        initProjectsComboBox()
        setProjectsComboBox(project)

        setHeaderDecorators()
    }

    /**
     * Configure Subscription combo box
     */
    private fun setSubscriptionComboBox() {
        cbSubscription.renderer = object : ListCellRendererWrapper<Subscription>() {
            override fun customize(list: JList<*>, subscription: Subscription?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                subscription ?: return
                setText(subscription.displayName())
            }
        }

        cbSubscription.addActionListener {
            val subscription = cbSubscription.getItemAt(cbSubscription.selectedIndex) ?: return@addActionListener
            val selectedSid = subscription.subscriptionId()
            if (lastSelectedSubscriptionId != selectedSid) {
                resetSubscriptionComboBoxValues()

                myView.onLoadResourceGroups(selectedSid)
                myView.onLoadLocation(selectedSid)
                myView.onLoadAppServicePlan(selectedSid)

                lastSelectedSubscriptionId = selectedSid
            }
        }
    }

    /**
     * Configure Resource Group combo box
     */
    private fun setResourceGroupComboBox() {
        cbResourceGroup.renderer = object : ListCellRendererWrapper<ResourceGroup>() {
            override fun customize(list: JList<*>, resourceGroup: ResourceGroup?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                resourceGroup ?: return
                setText(resourceGroup.name())
            }
        }

        cbResourceGroup.addActionListener {
            val selectedResourceGroup = cbResourceGroup.getItemAt(cbResourceGroup.selectedIndex) ?: return@addActionListener
            val groupName = selectedResourceGroup.name()
            if (lastSelectedResourceGroupName != groupName) {
                lastSelectedResourceGroupName = groupName
            }
        }
    }

    /**
     * Configure App Service Plan combo box
     */
    private fun setAppServicePlanComboBox() {
        cbAppServicePlan.renderer = object : ListCellRendererWrapper<AppServicePlan>() {
            override fun customize(list: JList<*>, appServicePlan: AppServicePlan?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                appServicePlan ?: return
                setText(appServicePlan.name())
            }
        }

        cbAppServicePlan.addActionListener {
            val plan = cbAppServicePlan.getItemAt(cbAppServicePlan.selectedIndex) ?: return@addActionListener
            lblLocation.text = plan.regionName()
            val skuDescription = plan.pricingTier().toSkuDescription()
            lblPricingTier.text = "${skuDescription.name()} (${skuDescription.tier()})"
        }
    }

    /**
     * Configure Operating System combo box
     */
    private fun setOperatingSystemComboBox() {
        cbOperatingSystem.renderer = object : ListCellRendererWrapper<OperatingSystem>() {
            override fun customize(list: JList<*>, operatingSystem: OperatingSystem?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                operatingSystem ?: return
                setText(operatingSystem.name.toLowerCase().capitalize())
            }
        }

        cbOperatingSystem.addActionListener {
            val operatingSystem = cbOperatingSystem.getItemAt(cbOperatingSystem.selectedIndex) ?: return@addActionListener
            toggleOperatingSystemComboBox(operatingSystem)
            lastSelectedOperatingSystem = operatingSystem
        }
    }

    /**
     * Configure Location combo box
     */
    private fun setLocationComboBox() {
        cbLocation.renderer = object : ListCellRendererWrapper<Location>() {
            override fun customize(list: JList<*>, location: Location?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                if (location != null) {
                    setText(location.displayName())
                }
            }
        }

        cbLocation.addActionListener {
            val location = cbLocation.getItemAt(cbLocation.selectedIndex) ?: return@addActionListener
            lastSelectedLocation = location.name()
        }
    }

    /**
     * Configure Pricing Tier combo box
     */
    private fun setPricingTierComboBox() {
        cbPricingTier.renderer = object : ListCellRendererWrapper<PricingTier>() {
            override fun customize(list: JList<*>?, pricingTier: PricingTier?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                pricingTier ?: return
                val skuDescription = pricingTier.toSkuDescription()
                setText("${skuDescription.name()} (${skuDescription.tier()})")
            }
        }

        cbPricingTier.addActionListener {
            val pricingTier = cbPricingTier.getItemAt(cbPricingTier.selectedIndex) ?: return@addActionListener
            lastSelectedPriceTier = pricingTier
        }
    }

    /**
     * Configure Runtime combo box
     */
    private fun setRuntimeComboBox() {
        cbRuntime.renderer = object : ListCellRendererWrapper<RuntimeStack>() {
            override fun customize(list: JList<*>, runtimeStack: RuntimeStack?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                runtimeStack ?: return
                setText(".Net Core ${runtimeStack.version()}")
            }
        }
        cbRuntime.addActionListener {
            val runtime = cbRuntime.getItemAt(cbRuntime.selectedIndex) ?: return@addActionListener
            lastSelectedRuntime = runtime
        }
    }

    /**
     * Configure Project combo box
     */
    private fun setProjectsComboBox(project: Project) {

        cbProject.renderer = object : ListCellRendererWrapper<PublishableProjectModel>() {
            override fun customize(list: JList<*>,
                                   publishableProject: PublishableProjectModel?,
                                   index: Int,
                                   isSelected: Boolean,
                                   cellHasFocus: Boolean) {
                if (project.isDisposed) return

                // Text
                if (publishableProject == null) {
                    setText(PROJECTS_EMPTY_MESSAGE)
                    return
                }

                setText(publishableProject.projectName)

                // Icon
                val projectVf = VfsUtil.findFileByIoFile(File(publishableProject.projectFilePath), false) ?: return

                val projectArray = ProjectModelViewHost.getInstance(project).getItemsByVirtualFile(projectVf)
                val projectNodes = projectArray.filter { it.isProject() || it.isUnloadedProject() }

                if (!projectNodes.isEmpty()) {
                    setIcon(projectNodes[0].getIcon())
                }
            }
        }

        cbProject.addActionListener {
            val publishableProject = cbProject.getItemAt(cbProject.selectedIndex) ?: return@addActionListener
            configuration.model.publishableProject = publishableProject
            configuration.name = "$RUN_CONFIG_PREFIX: ${publishableProject.projectName}"

            toggleProjectComboBox(publishableProject.isDotNetCore)
        }
    }

    /**
     * Reset Subscription combo box values from selected item
     */
    private fun resetSubscriptionComboBoxValues() {
        resetResourceGroupComboBoxValues()
        resetLocationComboBoxValues()
        resetAppServicePlanComboBoxValues()
    }

    /**
     * Reset Resource Group combo box values
     */
    private fun resetResourceGroupComboBoxValues() {
        cbResourceGroup.removeAllItems()
    }

    /**
     * Reset App Service Plan combo box values
     */
    private fun resetAppServicePlanComboBoxValues() {
        cbAppServicePlan.removeAllItems()
        lblLocation.text = NOT_APPLICABLE
        lblPricingTier.text = NOT_APPLICABLE
    }

    /**
     * Reset Location combo box values
     */
    private fun resetLocationComboBoxValues() {
        cbLocation.removeAllItems()
    }

    /**
     * Set Headers for panel groups
     */
    private fun setHeaderDecorators() {
        setResourceGroupDecorator()
        setAppServicePlanDecorator()
        setOperatingSystemDecorator()
        setRuntimeDecorator()
    }

    /**
     * Set header decorator for a Resource Group panel
     */
    private fun setResourceGroupDecorator() {
        setDecorator(HEADER_RESOURCE_GROUP, pnlResourceGroupHolder, pnlResourceGroup)
    }

    /**
     * Set header decorator for an App Service Plan panel
     */
    private fun setAppServicePlanDecorator() {
        setDecorator(HEADER_APP_SERVICE_PLAN, pnlAppServicePlanHolder, pnlAppServicePlan)
    }

    private fun setOperatingSystemDecorator() {
        setDecorator(HEADER_OPERATING_SYSTEM, pnlOperatingSystemHolder, pnlOperatingSystem)
    }

    private fun setRuntimeDecorator() {
        setDecorator(HEADER_RUNTIME, pnlRuntimeHolder, pnlRuntime)
    }

    /**
     * Set header decorator for a panel with a specified name
     *
     * @param name - decorator name
     * @param panelHolder - main panel holder for a decorator
     * @param content - panel content
     */
    private fun setDecorator(name: String, panelHolder: JPanel?, content: JComponent?) {
        val decorator = HideableDecorator(panelHolder, name, true)
        decorator.setContentComponent(content)
        decorator.setOn(true)
    }

    //endregion Configure UI Components

    //region Azure Model

    /**
     * Update cached values in [com.microsoft.azuretools.utils.AzureModel] in a background to use for fields validation on client side
     */
    private fun updateAzureModelInBackground() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureModelController.updateResourceGroupMaps(UpdateProgressIndicator(indicator))
            }
        })
    }

    //endregion Azure Model
}
