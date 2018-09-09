package com.microsoft.intellij.runner.db.dbconfig.ui

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.HideableDecorator
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.components.JBPasswordField
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.db.AzureDatabaseSettingModel
import com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseConfiguration
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

class RiderDatabaseSettingPanel(project: Project,
                                private val configuration: RiderDatabaseConfiguration) :
        AzureRiderSettingPanel<RiderDatabaseConfiguration>(project), DatabaseDeployMvpView {

    companion object {
        private const val HEADER_RESOURCE_GROUP = "Resource Group"
        private const val HEADER_SQL_SERVER = "SQL Server"
        private const val HEADER_PRICING_TIER = "Pricing Tier"
        private const val HEADER_COLLATION = "Collation"

        private const val DEFAULT_SQL_DATABASE_NAME = "sql_%s_db"
        private const val DEFAULT_RESOURCE_GROUP_NAME = "rg-db-"
        private const val DEFAULT_SQL_SERVER_NAME = "sql-server-"

        private const val DATABASE_SETTINGS_PANEL_NAME = "Deploy to SQL Database"
        private const val NOT_APPLICABLE = "N/A"
    }

    private var cachedResourceGroups = listOf<ResourceGroup>()
    private var lastSelectedSubscriptionId = ""
    private var lastSelectedResourceGroupName = ""
    private var lastSelectedSqlServer: SqlServer? = null
    private var lastSelectedLocation = ""
    private var lastSelectedDatabaseEdition = AzureDatabaseSettingModel.defaultDatabaseEditions

    private val myView: DatabaseDeployViewPresenter<RiderDatabaseSettingPanel> = DatabaseDeployViewPresenter()

    override val mainPanel: JPanel = pnlRoot
    private lateinit var pnlRoot: JPanel

    private lateinit var pnlCreate: JPanel
    private lateinit var txtDbName: JTextField

    private lateinit var cbSubscription: JComboBox<Subscription>

    private lateinit var pnlResourceGroupHolder: JPanel
    private lateinit var pnlResourceGroup: JPanel
    private lateinit var rdoUseExistResGrp: JRadioButton
    private lateinit var cbExistResGrp: JComboBox<ResourceGroup>
    private lateinit var rdoCreateResGrp: JRadioButton
    private lateinit var txtNewResGrp: JTextField

    private lateinit var pnlSqlServerHolder: JPanel
    private lateinit var pnlSqlServer: JPanel
    private lateinit var rdoUseExistSqlServer: JRadioButton
    private lateinit var cbExistSqlServer: JComboBox<SqlServer>
    private lateinit var lblExistingSqlServerAdminLogin: JLabel
    private lateinit var rdoCreateSqlServer: JRadioButton
    private lateinit var txtNewSqlServerName: JTextField
    private lateinit var txtNewSqlServerAdminLogin: JTextField
    private lateinit var passNewSqlServerAdminPass: JBPasswordField

    private lateinit var pnlPricingTierHolder: JPanel
    private lateinit var cbDatabaseEdition: JComboBox<DatabaseEditions>
    private lateinit var passNewSqlServerAdminPassConfirm: JBPasswordField
    private lateinit var cbLocation: JComboBox<Location>
    private lateinit var lblExistingSqlServerLocation: JLabel
    private lateinit var pnlPricingTier: JPanel

    private lateinit var pnlCollationHolder: JPanel
    private lateinit var pnlCollation: JPanel
    private lateinit var txtCollationValue: JTextField

    override val panelName: String
        get() = DATABASE_SETTINGS_PANEL_NAME

    init {
        myView.onAttachView(this)

        updateAzureDatabaseModelInBackground()

        initButtonGroupsState()
        setUIComponents()
    }

    //region Read From Config

    /**
     * Reset all controls from configuration.
     * Function is triggered while constructing the panel.
     *
     * @param configuration - Database Configuration instance
     */
    override fun resetFromConfig(configuration: RiderDatabaseConfiguration) {

        val model = configuration.model

        val dateFormat = SimpleDateFormat("yyMMddHHmmss")
        val date = dateFormat.format(Date())

        txtDbName.text = if (model.databaseName.isEmpty()) String.format(DEFAULT_SQL_DATABASE_NAME, date) else model.databaseName
        txtNewResGrp.text = if (model.resourceGroupName.isEmpty()) "$DEFAULT_RESOURCE_GROUP_NAME$date" else model.resourceGroupName
        txtNewSqlServerName.text = if (model.sqlServerName.isEmpty()) "$DEFAULT_SQL_SERVER_NAME$date" else model.sqlServerName
        txtCollationValue.text = model.collation

        if (model.isCreatingResourceGroup) {
            rdoCreateResGrp.doClick()
        } else {
            rdoUseExistResGrp.doClick()
        }

        if (model.isCreatingSqlServer) {
            rdoCreateSqlServer.doClick()
        } else {
            rdoUseExistSqlServer.doClick()
        }

        myView.onLoadSubscription()
        myView.onLoadDatabaseEdition()
    }

    /**
     * Function is triggered by any content change events.
     *
     * @param configuration - Database Configuration instance
     */
    override fun apply(configuration: RiderDatabaseConfiguration) {

        val model = configuration.model

        model.databaseName = txtDbName.text
        model.subscriptionId = lastSelectedSubscriptionId

        // Resource group
        if (rdoCreateResGrp.isSelected) {
            model.isCreatingResourceGroup = true
            model.resourceGroupName = txtNewResGrp.text
        } else {
            model.isCreatingResourceGroup = false
            model.resourceGroupName = lastSelectedResourceGroupName
        }

        // SQL Server
        if (rdoCreateSqlServer.isSelected) {
            model.isCreatingSqlServer = true
            model.sqlServerName = txtNewSqlServerName.text

            model.sqlServerAdminLogin = txtNewSqlServerAdminLogin.text
            model.sqlServerAdminPassword = passNewSqlServerAdminPass.password
            model.sqlServerAdminPasswordConfirm = passNewSqlServerAdminPassConfirm.password

            model.region = lastSelectedLocation
        } else {
            model.isCreatingSqlServer = false
            val sqlServer = cbExistSqlServer.getItemAt(cbExistSqlServer.selectedIndex)
            model.sqlServerId = sqlServer?.id() ?: ""
            model.sqlServerAdminLogin = lblExistingSqlServerAdminLogin.text
        }

        model.databaseEdition = lastSelectedDatabaseEdition
        model.collation = txtCollationValue.text
    }

    //endregion Read From Config

    //region MVP View

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
        cbExistResGrp.removeAllItems()
        if (resourceGroups.isEmpty()) {
            lastSelectedResourceGroupName = ""
            return
        }

        cachedResourceGroups = resourceGroups

        resourceGroups.sortedWith(compareBy { it.name() })
                .forEach {
                    cbExistResGrp.addItem(it)
                    if (it.name() == configuration.model.resourceGroupName) {
                        cbExistResGrp.selectedItem = it
                    }
                }
    }

    override fun fillSqlServer(sqlServers: List<SqlServer>) {
        cbExistSqlServer.removeAllItems()

        if (sqlServers.isEmpty()) {
            lastSelectedSqlServer = null
            return
        }

        sqlServers.sortedWith(compareBy { it.name() })
                .forEach {
                    cbExistSqlServer.addItem(it)
                    if (it.name() == configuration.model.sqlServerName) {
                        cbExistSqlServer.selectedItem = it
                        lastSelectedSqlServer = it
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
                .forEach {
                    cbLocation.addItem(it)
                    if (it.name() == configuration.model.region) {
                        cbLocation.selectedItem = it
                    }
                }
    }

    override fun fillDatabaseEdition(prices: List<DatabaseEditions>) {
        cbDatabaseEdition.removeAllItems()

        prices.forEach {
            cbDatabaseEdition.addItem(it)
            if (it == configuration.model.databaseEdition) {
                cbDatabaseEdition.selectedItem = it
            }
        }
    }

    override fun disposeEditor() {
        myView.onDetachView()
    }

    //endregion MVP View

    //region Button Groups Behavior

    private fun initButtonGroupsState() {
        initResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    /**
     * Button groups - Resource Group
     *
     * Note: If user choose to create a new Resource Group, then we restrict him from choosing existing SQL servers,
     *       because SQL Database cannot exists outside of SQL Server Resource Group
     */
    private fun initResourceGroupButtonGroup() {
        val btnGroupResourceGroup = ButtonGroup()
        btnGroupResourceGroup.add(rdoCreateResGrp)
        btnGroupResourceGroup.add(rdoUseExistResGrp)

        rdoCreateResGrp.addActionListener {
            toggleResourceGroupPanel(true)
            rdoCreateSqlServer.doClick()
        }

        rdoUseExistResGrp.addActionListener {
            if (rdoUseExistSqlServer.isSelected) return@addActionListener
            toggleResourceGroupPanel(false)
        }

        toggleResourceGroupPanel(true)
    }

    /**
     * Set Resources Group panels visibility when using existing or creating new Resource Group
     *
     * @param isCreatingNew - flag indicating whether we create new Resource Group or deploy to existing one
     */
    private fun toggleResourceGroupPanel(isCreatingNew: Boolean) {
        txtNewResGrp.isEnabled = isCreatingNew
        cbExistResGrp.isEnabled = !isCreatingNew
    }

    /**
     * Button groups - SQL Server
     *
     * Note: Existing SQL Server is already defined in some Resource Group. User has no option to choose
     *       a Resource Group if he selecting from existing SQL Servers.
     */
    private fun initSqlServerButtonsGroup() {
        val btnGroupSqlServer = ButtonGroup()
        btnGroupSqlServer.add(rdoCreateSqlServer)
        btnGroupSqlServer.add(rdoUseExistSqlServer)

        rdoCreateSqlServer.addActionListener {
            toggleSqlServerPanel(true)
            toggleResourceGroupPanel(rdoCreateResGrp.isSelected)
        }

        rdoUseExistSqlServer.addActionListener {
            toggleSqlServerPanel(false)

            val sqlServer = lastSelectedSqlServer
            if (sqlServer != null) toggleSqlServerComboBox(sqlServer)

            rdoUseExistResGrp.doClick()
            cbExistResGrp.isEnabled = false
        }

        toggleSqlServerPanel(true)
    }

    /**
     * Set SQL Server elements visibility when using existing or creating new SQL Server
     *
     * @param isCreatingNew - flag indicating whether we create new SQL Server or use and existing one
     */
    private fun toggleSqlServerPanel(isCreatingNew: Boolean) {
        cbExistSqlServer.isEnabled = !isCreatingNew

        txtNewSqlServerName.isEnabled = isCreatingNew
        txtNewSqlServerAdminLogin.isEnabled = isCreatingNew
        passNewSqlServerAdminPass.isEnabled = isCreatingNew
        passNewSqlServerAdminPassConfirm.isEnabled = isCreatingNew
        cbLocation.isEnabled = isCreatingNew
    }

    private fun toggleSqlServerComboBox(selectedSqlServer: SqlServer) {
        val resourceGroupToSet =
                cachedResourceGroups.find { it.name() == selectedSqlServer.resourceGroupName() } ?: return

        cbExistResGrp.selectedItem = resourceGroupToSet
    }

    //endregion Button Groups Behavior

    //region Configure UI Components

    /**
     * Configure renderer and listeners for all UI Components
     */
    private fun setUIComponents() {
        setSubscriptionComboBox()
        setResourceGroupComboBox()
        setSqlServerComboBox()
        setLocationComboBox()
        setDatabaseEditionComboBox()

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
            if (selectedSid != lastSelectedSubscriptionId) {
                resetSubscriptionComboBoxValues()

                myView.onLoadResourceGroups(selectedSid)
                myView.onLoadSqlServers(selectedSid)
                myView.onLoadLocation(selectedSid)

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
            val resourceGroup = cbExistResGrp.getItemAt(cbExistResGrp.selectedIndex) ?: return@addActionListener
            val resourceGroupName = resourceGroup.name()

            if (resourceGroupName != lastSelectedResourceGroupName) {
                lastSelectedResourceGroupName = resourceGroupName
            }
        }
    }

    /**
     * Configure SQL Server combo box
     */
    private fun setSqlServerComboBox() {
        cbExistSqlServer.renderer = object : ListCellRendererWrapper<SqlServer>() {
            override fun customize(list: JList<*>, sqlServer: SqlServer?, index: Int, selected: Boolean, hasFocus: Boolean) {
                sqlServer ?: return
                setText("${sqlServer.name()} (${sqlServer.resourceGroupName()})")
            }
        }

        cbExistSqlServer.addActionListener {
            val sqlServer = cbExistSqlServer.getItemAt(cbExistSqlServer.selectedIndex) ?: return@addActionListener

            if (sqlServer != lastSelectedSqlServer) {
                lblExistingSqlServerLocation.text = sqlServer.region().label()
                lblExistingSqlServerAdminLogin.text = sqlServer.administratorLogin()
                lastSelectedSqlServer = sqlServer

                toggleSqlServerComboBox(sqlServer)
            }
        }
    }

    /**
     * Configure Location combo box
     */
    private fun setLocationComboBox() {
        cbLocation.renderer = object : ListCellRendererWrapper<Location>() {
            override fun customize(list: JList<*>, location: Location?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
                location ?: return
                setText(location.displayName())
            }
        }

        cbLocation.addActionListener {
            val location = cbLocation.getItemAt(cbLocation.selectedIndex) ?: return@addActionListener
            lastSelectedLocation = location.name()
        }
    }

    /**
     * Configure Database Edition combo box
     */
    private fun setDatabaseEditionComboBox() {
        cbDatabaseEdition.addActionListener {
            val databaseEdition = cbDatabaseEdition.getItemAt(cbDatabaseEdition.selectedIndex) ?: return@addActionListener
            lastSelectedDatabaseEdition = databaseEdition
        }
    }

    /**
     * Reset Subscription combo box values from selected item
     */
    private fun resetSubscriptionComboBoxValues() {
        resetResourceGroupComboBoxValues()
        resetLocationComboBoxValues()
    }

    /**
     * Reset Resource Group combo box values from selected item
     */
    private fun resetResourceGroupComboBoxValues() {
        cbExistSqlServer.removeAllItems()
        resetSqlServerComboBoxValues()
    }

    /**
     * Reset Location combo box values
     */
    private fun resetLocationComboBoxValues() {
        cbLocation.removeAllItems()
    }

    /**
     * Reset SQL Server combo box values from selected item
     */
    private fun resetSqlServerComboBoxValues() {
        lblExistingSqlServerLocation.text = NOT_APPLICABLE
        lblExistingSqlServerAdminLogin.text = NOT_APPLICABLE
    }

    /**
     * Set Headers for panel groups
     */
    private fun setHeaderDecorators() {
        setResourceGroupDecorator()
        setSqlServerDecorator()
        setPricingTierDecorator()
        setCollationDecorator()
    }

    /**
     * Set header decorator for a Resource Group panel
     */
    private fun setResourceGroupDecorator() {
        setDecorator(HEADER_RESOURCE_GROUP, pnlResourceGroupHolder, pnlResourceGroup)
    }

    /**
     * Set header decorator for a SQL Server panel
     */
    private fun setSqlServerDecorator() {
        setDecorator(HEADER_SQL_SERVER, pnlSqlServerHolder, pnlSqlServer)
    }

    /**
     * Set header decorator for Pricing Tier
     */
    private fun setPricingTierDecorator() {
        setDecorator(HEADER_PRICING_TIER, pnlPricingTierHolder, pnlPricingTier)
    }

    /**
     * Set header decorator for Collation
     */
    private fun setCollationDecorator() {
        setDecorator(HEADER_COLLATION, pnlCollationHolder, pnlCollation)
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
    private fun updateAzureDatabaseModelInBackground() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureDatabaseMvpModel.refreshSqlDatabaseToSqlDatabaseMap()
            }
        })
    }

    //endregion Azure Model
}
