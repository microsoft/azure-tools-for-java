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
import com.intellij.ui.*
import com.intellij.ui.table.JBTable
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.nodes.isProject
import com.jetbrains.rider.projectView.nodes.isUnloadedProject
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.lifetime
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.webapp.JdkModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.azuretools.utils.WebAppUtils
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppConfiguration
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

/**
 * The setting panel for web app deployment run configuration.
 */
class RiderWebAppSettingPanel(project: Project,
                              private val configuration: RiderWebAppConfiguration)
    : AzureRiderSettingPanel<RiderWebAppConfiguration>(project), WebAppDeployMvpView {

    companion object {

        // const
        private const val WEB_APP_SETTINGS_PANEL_NAME = "Run On Web App"

        private const val RUN_CONFIG_PREFIX = "Azure Web App"

        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_APP_SERVICE_PLAN = "App Service Plan"

        private const val WEB_APP_TABLE_COLUMN_SUBSCRIPTION = "Subscription"
        private const val WEB_APP_TABLE_COLUMN_NAME = "Name"
        private const val WEB_APP_TABLE_COLUMN_RESOURCE_GROUP = "Resource group"
        private const val WEB_APP_TABLE_COLUMN_LOCATION = "Location"

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
    private val myView = WebAppDeployViewPresenter<RiderWebAppSettingPanel>()

    // cache variable
    private var selectedWebApp: ResourceEx<WebApp>? = null
    private var cachedWebAppList = listOf<ResourceEx<WebApp>>()
    private var lastSelectedSubscriptionId: String? = null
    private var lastSelectedResourceGroupName: String? = null
    private var lastSelectedLocation: String? = null
    private var lastSelectedPriceTier: PricingTier? = null

    // widgets
    override var mainPanel: JPanel = pnlRoot
    private lateinit var pnlRoot: JPanel
    private lateinit var pnlExist: JPanel
    private lateinit var pnlCreate: JPanel
    private lateinit var pnlWebAppTable: JPanel
    private lateinit var rdoUseExist: JRadioButton
    private lateinit var rdoCreateNew: JRadioButton
    private lateinit var rdoCreateAppServicePlan: JRadioButton
    private lateinit var rdoUseExistAppServicePlan: JRadioButton
    private lateinit var rdoCreateResGrp: JRadioButton
    private lateinit var rdoUseExistResGrp: JRadioButton
    private lateinit var txtWebAppName: JTextField
    private lateinit var txtCreateAppServicePlan: JTextField
    private lateinit var txtNewResGrp: JTextField
    private lateinit var txtSelectedWebApp: JTextField
    private lateinit var cbSubscription: JComboBox<Subscription>
    private lateinit var cbLocation: JComboBox<Location>
    private lateinit var cbPricing: JComboBox<PricingTier>
    private lateinit var cbExistAppServicePlan: JComboBox<AppServicePlan>
    private lateinit var cbExistResGrp: JComboBox<ResourceGroup>
    private lateinit var lblLocation: JLabel
    private lateinit var lblPricing: JLabel
    private lateinit var lblProject: JLabel
    private lateinit var pnlResourceGroupHolder: JPanel
    private lateinit var pnlResourceGroup: JPanel
    private lateinit var pnlAppServicePlanHolder: JPanel
    private lateinit var pnlAppServicePlan: JPanel
    private lateinit var pnlProject: JPanel
    private lateinit var cbProject: JComboBox<PublishableProjectModel>
    private lateinit var table: JBTable
    private lateinit var btnRefresh: AnActionButton

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

        txtWebAppName.text = model.webAppName ?: "$DEFAULT_APP_NAME$dateString"
        txtCreateAppServicePlan.text = model.appServicePlanName ?: "$DEFAULT_PLAN_NAME$dateString"
        txtNewResGrp.text = model.resourceGroupName ?: "$DEFAULT_RGP_NAME$dateString"

        if (model.isCreatingWebApp) {
            rdoCreateNew.doClick()
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
            rdoUseExist.doClick()
        }
        btnRefresh.isEnabled = false

        myView.onLoadSubscription()
        myView.onLoadWebApps()
        myView.onLoadPricingTier()
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

        if (rdoUseExist.isSelected) {
            model.isCreatingWebApp = false
            model.subscriptionId = selectedWebApp?.subscriptionId
            model.webAppId = selectedWebApp?.resource?.id()
        } else if (rdoCreateNew.isSelected) {
            model.isCreatingWebApp = true

            model.webAppName = txtWebAppName.text
            model.subscriptionId = lastSelectedSubscriptionId

            // Resource group
            if (rdoCreateResGrp.isSelected) {
                model.isCreatingResourceGroup = true
                model.resourceGroupName = txtNewResGrp.text
            } else {
                model.isCreatingResourceGroup = false
                model.resourceGroupName = lastSelectedResourceGroupName
            }

            // App service plan
            if (rdoCreateAppServicePlan.isSelected) {
                model.isCreatingAppServicePlan = true
                model.appServicePlanName = txtCreateAppServicePlan.text

                model.region = lastSelectedLocation
                model.pricingTier = lastSelectedPriceTier
            } else {
                model.isCreatingAppServicePlan = false
                val appServicePlan = cbExistAppServicePlan.getItemAt(cbExistAppServicePlan.selectedIndex)
                if (appServicePlan != null) {
                    model.appServicePlanId = appServicePlan.id()
                }
            }
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

        val sortedList = webAppLists.sortedWith(Comparator { left, right ->
            left.subscriptionId.compareTo(right.subscriptionId, ignoreCase = true)
        })

        cachedWebAppList = sortedList
        if (sortedList.isNotEmpty()) {
            val model = table.model as DefaultTableModel
            model.dataVector.clear()

            var subscriptions: List<Subscription> = ArrayList()
            try {
                subscriptions = AuthMethodManager.getInstance().azureManager.subscriptions
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

            for (i in sortedList.indices) {
                val app = sortedList[i].resource
                val subscriptionName = subscriptions
                        .filter { it.subscriptionId() == app.manager().subscriptionId() }
                        .map { it.displayName() }
                        .firstOrNull() ?: ""
                model.addRow(arrayOf(app.name(), app.resourceGroupName(), app.region().name(), subscriptionName))
                if (app.id() == configuration.model.webAppId) {
                    table.setRowSelectionInterval(i, i)
                }
            }
        }
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        cbSubscription.removeAllItems()
        if (subscriptions.isEmpty()) {
            lastSelectedSubscriptionId = null
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
        cbExistResGrp.removeAllItems()
        if (resourceGroups.isEmpty()) {
            lastSelectedResourceGroupName = null
            return
        }

        resourceGroups.sortedWith(compareBy { it.name() })
                .forEach {
                    cbExistResGrp.addItem(it)
                    if (it.name() == configuration.model.resourceGroupName) {
                        cbExistResGrp.selectedItem = it
                    }
                }
    }

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {
        cbExistAppServicePlan.removeAllItems()
        if (appServicePlans.isEmpty()) {
            lblLocation.text = NOT_APPLICABLE
            lblPricing.text = NOT_APPLICABLE
            return
        }
        appServicePlans.filter { it.operatingSystem() == OperatingSystem.WINDOWS }
                .sortedWith(compareBy { it.name() })
                .forEach {
                    cbExistAppServicePlan.addItem(it)
                    if (it.id() == configuration.model.appServicePlanId) {
                        cbExistAppServicePlan.selectedItem = it
                    }
                }
    }

    override fun fillLocation(locations: List<Location>) {
        cbLocation.removeAllItems()
        if (locations.isEmpty()) {
            lastSelectedLocation = null
            return
        }
        locations.sortedWith(compareBy { it.displayName() })
                .forEach { location ->
                    cbLocation.addItem(location)
                    if (location.name() == configuration.model.region) {
                        cbLocation.selectedItem = location
                    }
                }
    }

    override fun fillPricingTier(prices: List<PricingTier>) {
        cbPricing.removeAllItems()
        prices.forEach {
            cbPricing.addItem(it)
            if (it == configuration.model.pricingTier) {
                cbPricing.selectedItem = it
            }
        }
    }

    override fun fillWebContainer(webContainers: List<WebAppUtils.WebContainerMod>) {}

    override fun fillJdkVersion(jdks: List<JdkModel>) {}

    /**
     * File cbProject with available projects in a solution
     *
     * @param publishableProjects - publishable projects in a current solution
     */
    private fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        cbProject.removeAllItems()

        filterProjects(publishableProjects).sortedBy { it.projectName }
                .forEach { cbProject.addItem(it)
                    if (it == configuration.model.publishableProject) {
                        cbProject.selectedItem = it
                    }
                }
    }

    /**
     * Filter all non-core apps or non-web apps on full .Net framework
     *
     * @param publishableProjects list of all available publishable projects
     */
    private fun filterProjects(publishableProjects: Collection<PublishableProjectModel>): List<PublishableProjectModel> {
        return publishableProjects.filter { it.isWeb && (it.isDotNetCore || SystemInfo.isWindows) }
    }

    /**
     * Let the presenter release the view. Will be called by:
     * [com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppSettingEditor.disposeEditor].
     */
    override fun disposeEditor() {
        myView.onDetachView()
    }

    //endregion MVP View

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
        btnGrpForDeploy.add(rdoUseExist)
        btnGrpForDeploy.add(rdoCreateNew)
        rdoUseExist.addActionListener { toggleDeployPanel(true) }
        rdoCreateNew.addActionListener { toggleDeployPanel(false) }
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
        pnlExist.isVisible = isUsingExisting
        pnlCreate.isVisible = !isUsingExisting
    }

    private fun toggleResGrpPanel(isCreatingNew: Boolean) {
        txtNewResGrp.isEnabled = isCreatingNew
        cbExistResGrp.isEnabled = !isCreatingNew
    }

    private fun toggleAppServicePlanPanel(isCreatingNew: Boolean) {
        txtCreateAppServicePlan.isEnabled = isCreatingNew
        cbLocation.isEnabled = isCreatingNew
        cbPricing.isEnabled = isCreatingNew
        cbExistAppServicePlan.isEnabled = !isCreatingNew
        lblLocation.isEnabled = !isCreatingNew
        lblPricing.isEnabled = !isCreatingNew
    }

    //endregion Button Groups Behavior

    //region Configure UI Components

    /**
     * Configure renderer and listeners for all UI Components
     */
    private fun setUIComponents(project: Project) {
        setSubscriptionComboBox()
        setResourceGroupComboBox()
        setAppServicePlanComboBox()
        setLocationComboBox()
        setPricingTierComboBox()

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
        cbExistResGrp.renderer = object : ListCellRendererWrapper<ResourceGroup>() {
            override fun customize(list: JList<*>, resourceGroup: ResourceGroup?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                resourceGroup ?: return
                setText(resourceGroup.name())
            }
        }

        cbExistResGrp.addActionListener {
            val selectedResourceGroup = cbExistResGrp.getItemAt(cbExistResGrp.selectedIndex) ?: return@addActionListener
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
        cbExistAppServicePlan.renderer = object : ListCellRendererWrapper<AppServicePlan>() {
            override fun customize(list: JList<*>, appServicePlan: AppServicePlan?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                if (appServicePlan != null) {
                    setText(appServicePlan.name())
                }
            }
        }

        cbExistAppServicePlan.addActionListener {
            val plan = cbExistAppServicePlan.getItemAt(cbExistAppServicePlan.selectedIndex) ?: return@addActionListener
            lblLocation.text = plan.regionName()
            lblPricing.text = plan.pricingTier().toString()
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
        cbPricing.addActionListener {
            val pricingTier = cbPricing.getItemAt(cbPricing.selectedIndex) ?: return@addActionListener
            lastSelectedPriceTier = pricingTier
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
        cbExistResGrp.removeAllItems()
    }

    /**
     * Reset App Service Plan combo box values
     */
    private fun resetAppServicePlanComboBoxValues() {
        cbExistAppServicePlan.removeAllItems()
        lblLocation.text = NOT_APPLICABLE
        lblPricing.text = NOT_APPLICABLE
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
