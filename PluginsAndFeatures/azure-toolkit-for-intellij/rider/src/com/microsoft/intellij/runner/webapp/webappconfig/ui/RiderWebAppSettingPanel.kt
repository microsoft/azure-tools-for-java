package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.AnActionButton
import com.intellij.ui.HideableDecorator
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.table.JBTable
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.projectModelTasks
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
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseConfigurationType
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

/**
 * The setting panel for web app deployment run configuration.
 *
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
        private const val WEB_APP_RUNTIME_MISMATCH_WARNING =
                "Selected Azure WebApp runtime '%s' mismatch with Project .Net Core Framework '%s'"

        private const val BUTTON_REFRESH_NAME = "Refresh"

        private const val NOT_APPLICABLE = "N/A"
        private const val TABLE_LOADING_MESSAGE = "Loading ... "
        private const val TABLE_EMPTY_MESSAGE = "No available Web Apps."
        private const val PROJECTS_EMPTY_MESSAGE = "No projects to publish"

        private const val DATABASES_EMPTY_MESSAGE = "No Azure SQL database"

        private const val DEFAULT_APP_NAME = "webapp-"
        private const val DEFAULT_PLAN_NAME = "appsp-"
        private const val DEFAULT_RGP_NAME = "rg-webapp-"

        private val netCoreAppVersionRegex = Regex("\\.NETCoreApp,Version=v([0-9](?:\\.[0-9])*)", RegexOption.IGNORE_CASE)

        private val informationIcon = com.intellij.icons.AllIcons.General.BalloonInformation
        private val warningIcon = com.intellij.icons.AllIcons.General.BalloonWarning
        private val commitIcon = com.intellij.icons.AllIcons.Actions.Commit
    }

    // presenter
    private val myView = DotNetWebAppDeployViewPresenter<RiderWebAppSettingPanel>()

    // cache variable
    private var lastSelectedProject: PublishableProjectModel? = null

    private var lastSelectedWebApp: ResourceEx<WebApp>? = null
    private var cachedWebAppList = listOf<ResourceEx<WebApp>>()

    private var lastSelectedSubscriptionId = ""

    private var lastSelectedResourceGroupName = ""

    private var lastSelectedOperatingSystem = AzureDotNetWebAppSettingModel.defaultOperatingSystem

    private var cachedAppServicePlan = listOf<AppServicePlan>()
    private var lastSelectedLocation = ""
    private var cachedPricingTier = listOf<PricingTier>()
    private var lastSelectedPriceTier = AzureDotNetWebAppSettingModel.defaultPricingTier

    private var lastSelectedRuntime = AzureDotNetWebAppSettingModel.defaultRuntime

    private var lastSelectedDatabase: SqlDatabase? = null

    // Panels
    override var mainPanel: JPanel = pnlRoot
    private lateinit var pnlRoot: JPanel
    private lateinit var tpRoot: JBTabbedPane
    private lateinit var pnlWebAppConfigTab: JPanel
    private lateinit var pnlDbConnectionTab: JPanel

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

    // SQL Database
    private lateinit var pnlDbConnection: JPanel
    private lateinit var checkBoxEnableDbConnection: JCheckBox
    private lateinit var pnlDbConnectionStringSettings: JPanel
    private lateinit var txtConnectionStringName: JTextField
    private lateinit var cbDatabase: JComboBox<SqlDatabase>
    private lateinit var lblSqlDbAdminLogin: JLabel
    private lateinit var passSqlDbAdminPassword: JBPasswordField

    private lateinit var pnlSqlDatabaseRunConfigInfo: JPanel
    private lateinit var lblSqlDatabaseRunConfigInfo: JLabel

    private lateinit var lblRuntimeMismatchWarning: JLabel

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

        cbProject.selectedItem = model.publishableProject

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

        // TODO: Reset checkBoxEnableDbConnection from configuration

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

        model.publishableProject = lastSelectedProject

        if (rdoUseExistingWebApp.isSelected) {
            model.isCreatingWebApp = false
            model.subscriptionId = lastSelectedWebApp?.subscriptionId ?: ""
            model.webAppId = lastSelectedWebApp?.resource?.id() ?: ""
            model.operatingSystem = lastSelectedWebApp?.resource?.operatingSystem() ?: OperatingSystem.WINDOWS
            if (model.operatingSystem == OperatingSystem.LINUX) {
                val dotNetCoreVersionArray = lastSelectedWebApp?.resource?.linuxFxVersion()?.split('|')
                val runtime =
                        if (dotNetCoreVersionArray != null && dotNetCoreVersionArray.size == 2) RuntimeStack(dotNetCoreVersionArray[0], dotNetCoreVersionArray[1])
                        else AzureDotNetWebAppSettingModel.defaultRuntime
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

        if (checkBoxEnableDbConnection.isSelected) {
            model.isDatabaseConnectionEnabled = true
            model.connectionStringName = txtConnectionStringName.text
            model.database = lastSelectedDatabase
            model.sqlDatabaseAdminLogin = lblSqlDbAdminLogin.text
            model.sqlDatabaseAdminPassword = passSqlDbAdminPassword.password
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

        if (sortedWebApps.isEmpty()) return

        collectConnectionStringsInBackground(webAppLists.map { it.resource })

        val model = table.model as DefaultTableModel
        model.dataVector.clear()

        val subscriptionManager = AuthMethodManager.getInstance().azureManager.subscriptionManager

        for (i in sortedWebApps.indices) {
            val webApp = sortedWebApps[i].resource
            val subscription = subscriptionManager.subscriptionIdToSubscriptionMap[webApp.manager().subscriptionId()] ?: continue

            model.addRow(arrayOf(
                    webApp.name(),
                    webApp.resourceGroupName(),
                    webApp.region().label(),
                    webApp.operatingSystem().name.toLowerCase().capitalize(),
                    getNetFrameworkVersion(webApp),
                    subscription.displayName())
            )

            if (webApp.id() == configuration.model.webAppId ||
                    (lastSelectedWebApp != null && lastSelectedWebApp?.resource?.id() == webApp.id())) {
                table.setRowSelectionInterval(i, i)
            }
        }
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
            toggleResourceGroupPanel(true)
            allowExistingResourceGroups(false)
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
            toggleAppServicePlanPanel(true)
            allowExistingAppServicePlans(false)
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

    override fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        cbProject.removeAllItems()

        val filteredProjects = filterProjects(publishableProjects)
        filteredProjects.sortedBy { it.projectName }
                .forEach {
                    cbProject.addItem(it)
                    if (it == configuration.model.publishableProject) {
                        cbProject.selectedItem = it
                        lastSelectedProject = it
                        setOperatingSystemComboBoxState(it)
                    }
                }
    }

    override fun fillSqlDatabase(database: List<SqlDatabase>) {
        cbDatabase.removeAllItems()

        if (database.isEmpty()) {
            lastSelectedDatabase = null
            return
        }
        database.sortedBy { it.name() }
                .forEach {
                    cbDatabase.addItem(it)
                    if (it == configuration.model.database) {
                        cbDatabase.selectedItem = it
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

    /**
     * Filter Web App table content based on selected project
     *
     * @param publishableProject a selected project
     */
    private fun filterWebAppTableContent(publishableProject: PublishableProjectModel) {
        val sorter = table.rowSorter as? TableRowSorter<*> ?: return

        if (publishableProject.isDotNetCore) {
            sorter.rowFilter = null
            return
        }

        val osColumnIndex = (table.model as DefaultTableModel).findColumn(WEB_APP_TABLE_COLUMN_OS)
        assert(osColumnIndex >= 0)
        sorter.rowFilter = javax.swing.RowFilter.regexFilter(OperatingSystem.WINDOWS.name.toLowerCase().capitalize(), osColumnIndex)
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
                lastSelectedWebApp = null
                txtSelectedWebApp.text = ""
                return@addListSelectionListener
            }

            val webApp = cachedWebAppList[selectedRow]
            lastSelectedWebApp = webApp

            // This is not visible on the UI, but is used to preform a re-validation over selected web app from the table
            txtSelectedWebApp.text = webApp.resource.name()

            val publishableProject = cbProject.getItemAt(cbProject.selectedIndex) ?: return@addListSelectionListener
            checkSelectedProjectAgainstWebAppRuntime(webApp.resource, publishableProject)
        }

        table.rowSorter = TableRowSorter<TableModel>(table.model)
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
        setRuntimeMismatchWarning(false)
    }

    private fun setRuntimeMismatchWarning(show: Boolean, message: String = "") {
        lblRuntimeMismatchWarning.text = message
        lblRuntimeMismatchWarning.isVisible = show
    }

    private fun checkSelectedProjectAgainstWebAppRuntime(webApp: WebApp, publishableProject: PublishableProjectModel) {
        if (webApp.operatingSystem() == OperatingSystem.WINDOWS) {
            setRuntimeMismatchWarning(false)
            return
        }

        // DOTNETCORE|2.0 -> 2.0
        val webAppFrameworkVersion = webApp.linuxFxVersion().split('|').getOrNull(1)

        // .NETCoreApp,Version=v2.0 -> 2.0
        val projectNetCoreVersion = getProjectTargetFramework(publishableProject)
        setRuntimeMismatchWarning(
                webAppFrameworkVersion != projectNetCoreVersion,
                String.format(WEB_APP_RUNTIME_MISMATCH_WARNING, webAppFrameworkVersion, projectNetCoreVersion))
    }

    private fun getProjectTargetFramework(publishableProject: PublishableProjectModel): String {
        val targetFramework = project.solution.projectModelTasks.targetFrameworks[publishableProject.projectModelId]
        val targetFrameworkId = targetFramework?.currentTargetFrameworkId?.valueOrNull ?: return ""
        return netCoreAppVersionRegex.find(targetFrameworkId)?.groups?.get(1)?.value ?: ""
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
        rdoCreateNewWebApp.addActionListener { toggleDeployPanel(true) }
        rdoUseExistingWebApp.addActionListener { toggleDeployPanel(false) }
        toggleDeployPanel(false)
    }

    private fun initResourceGroupButtonGroup() {
        val btnGrpForResGrp = ButtonGroup()
        btnGrpForResGrp.add(rdoUseExistResGrp)
        btnGrpForResGrp.add(rdoCreateResGrp)
        rdoCreateResGrp.addActionListener { toggleResourceGroupPanel(true) }
        rdoUseExistResGrp.addActionListener { toggleResourceGroupPanel(false) }
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

    private fun initDbConnectionEnableCheckbox() {
        checkBoxEnableDbConnection.addActionListener { toggleDbConnectionEnable(checkBoxEnableDbConnection.isSelected) }
    }

    private fun toggleDeployPanel(isCreatingNew: Boolean) {
        pnlCreateWebApp.isVisible = isCreatingNew
        pnlExistingWebApp.isVisible = !isCreatingNew
    }

    private fun toggleResourceGroupPanel(isCreatingNew: Boolean) {
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

    private fun showRuntimePanel(isLinux: Boolean) {
        pnlRuntimeHolder.isVisible = isLinux
    }

    private fun setOperatingSystemComboBoxState(publishableProject: PublishableProjectModel) {
        cbOperatingSystem.isEnabled = publishableProject.isDotNetCore
        if (!publishableProject.isDotNetCore) { cbOperatingSystem.selectedItem = OperatingSystem.WINDOWS }
    }

    private fun setRuntimeComboBoxState(publishableProject: PublishableProjectModel) {
        if (!publishableProject.isDotNetCore) return

        val netCoreVersion = getProjectTargetFramework(publishableProject)
        val runtimeIndex = (cbRuntime.model as? DefaultComboBoxModel)?.getIndexOf(RuntimeStack("DOTNETCORE", netCoreVersion)) ?: return
        val desiredRuntime = cbRuntime.getItemAt(runtimeIndex) ?: return
        cbRuntime.selectedItem = desiredRuntime
    }

//    private fun setRuntimeComboBoxRenderer(publishableProject: PublishableProjectModel) {
//        if (!publishableProject.isDotNetCore) return
//
//        cbRuntime.renderer = object : ListCellRendererWrapper<RuntimeStack>() {
//            override fun customize(list: JList<*>?, runtimeStack: RuntimeStack?, index: Int, selected: Boolean, hasFocus: Boolean) {
//
//                val selectedRuntime = cbRuntime.getItemAt(cbRuntime.selectedIndex) ?: return
//                val webAppRuntime = selectedRuntime.version()
//
//                val projectFrameworkVersion = getProjectTargetFramework(publishableProject)
//                setIcon(warningIcon)
//                setToolTipText(String.format(WEB_APP_RUNTIME_MISMATCH_WARNING, webAppRuntime, projectFrameworkVersion))
//
//                setNewWebAppRuntimeMismatchWarning(
//                        webAppRuntime != projectFrameworkVersion,
//                        )
//            }
//        }
//    }

    private fun toggleDbConnectionEnable(isSelected: Boolean) {
        txtConnectionStringName.isEnabled = isSelected
        cbDatabase.isEnabled = isSelected
        passSqlDbAdminPassword.isEnabled = isSelected
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

        initDbConnectionEnableCheckbox()
        setSqlDatabaseComboBox()
        setSqlDatabaseRunConfigInfoLabel()

        setRuntimeMismatchWarningLabel()

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
                myView.onLoadSqlDatabase(selectedSid)

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
            lastSelectedResourceGroupName = selectedResourceGroup.name()
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

            setAppServicePlanContent(filterAppServicePlans(operatingSystem, cachedAppServicePlan))
            setPricingTierContent(filterPricingTiers(operatingSystem, cachedPricingTier))
            showRuntimePanel(operatingSystem == OperatingSystem.LINUX)

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

                val webAppRuntime = runtimeStack.version()

                val publishableProject = cbProject.getItemAt(cbProject.selectedIndex)
                val projectFrameworkVersion = getProjectTargetFramework(publishableProject)

                if (projectFrameworkVersion != webAppRuntime) {
                    setIcon(warningIcon)
                    setToolTipText(String.format(WEB_APP_RUNTIME_MISMATCH_WARNING, webAppRuntime, projectFrameworkVersion))
                } else {
                    setIcon(commitIcon)
                    setToolTipText(null)
                }
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
            if (publishableProject == lastSelectedProject) return@addActionListener

            setOperatingSystemComboBoxState(publishableProject)
            setRuntimeComboBoxState(publishableProject)
//            setRuntimeComboBoxRenderer(publishableProject)
            filterWebAppTableContent(publishableProject)

            val webApp = lastSelectedWebApp?.resource
            if (webApp != null) checkSelectedProjectAgainstWebAppRuntime(webApp, publishableProject)

            lastSelectedProject = publishableProject
            setConfigurationName("$RUN_CONFIG_PREFIX: ${publishableProject.projectName}")
        }
    }

    private fun setConfigurationName(name: String) {
        configuration.name = name
    }

    /**
     * Configure SQL Database combo box
     */
    private fun setSqlDatabaseComboBox() {

        cbDatabase.renderer = object : ListCellRendererWrapper<SqlDatabase>() {
            override fun customize(list: JList<*>?, sqlDatabase: SqlDatabase?, index: Int, selected: Boolean, hasFocus: Boolean) {
                if (sqlDatabase == null) {
                    setText(DATABASES_EMPTY_MESSAGE)
                    return
                }

                setText("${sqlDatabase.name()} (${sqlDatabase.resourceGroupName()})")
                setIcon(IconLoader.getIcon("icons/Database.svg"))
            }
        }

        cbDatabase.addActionListener {
            val database = cbDatabase.getItemAt(cbDatabase.selectedIndex) ?: return@addActionListener
            if (lastSelectedDatabase != database) {
                AzureDatabaseMvpModel.getSqlServerAdminLoginAsync(lastSelectedSubscriptionId, database).subscribe { lblSqlDbAdminLogin.text = it }
                lastSelectedDatabase = database
            }
        }
    }

    private fun setSqlDatabaseRunConfigInfoLabel() {
        lblSqlDatabaseRunConfigInfo.text = "Please see '${RiderDatabaseConfigurationType.instance.displayName}' run configuration to create a new Azure SQL Database"
        lblSqlDatabaseRunConfigInfo.icon = informationIcon
    }

    private fun setRuntimeMismatchWarningLabel() {
        lblRuntimeMismatchWarning.icon = warningIcon
    }

    /**
    * Reset Subscription combo box values from selected item
    */
    private fun resetSubscriptionComboBoxValues() {
        resetResourceGroupComboBoxValues()
        resetLocationComboBoxValues()
        resetAppServicePlanComboBoxValues()
        resetSqlDatabaseComboBoxValues()
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

    private fun resetSqlDatabaseComboBoxValues() {
        cbDatabase.removeAllItems()
        lblSqlDbAdminLogin.text = NOT_APPLICABLE
        passSqlDbAdminPassword.text = ""
    }

    private fun allowExistingResourceGroups(isAllowed: Boolean) {
        rdoUseExistResGrp.isEnabled = isAllowed
        cbResourceGroup.isEnabled = isAllowed
    }

    private fun allowExistingAppServicePlans(isAllowed: Boolean) {
        rdoUseExistAppServicePlan.isEnabled = isAllowed
        cbAppServicePlan.isEnabled = isAllowed
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

    private fun collectConnectionStringsInBackground(webApps: List<WebApp>) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Web App connection strings map", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                webApps.forEach {
                    AzureDotNetWebAppMvpModel.getConnectionStrings(it, true)
                }
            }
        })
    }

    //endregion Azure Model
}
