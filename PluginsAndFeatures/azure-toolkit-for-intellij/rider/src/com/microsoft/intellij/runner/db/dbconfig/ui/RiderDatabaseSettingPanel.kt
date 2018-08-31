package com.microsoft.intellij.runner.db.dbconfig.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.ui.HideableDecorator
import com.intellij.ui.ListCellRendererWrapper
import com.intellij.ui.components.JBPasswordField
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.intellij.runner.AzureRiderSettingPanel
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

        private const val RUN_CONFIG_PREFIX = "Azure SQL Database"
    }

    // cache variable
    private var lastSelectedSubscriptionId: String? = null
    private var lastSelectedResourceGroupName: String? = null
    private var lastSelectedSqlServer: String? = null
    private var lastSelectedLocation: String? = null
    private var lastSelectedDatabaseEdition: DatabaseEditions? = null

    // presenter
    private val myView: DatabaseDeployViewPresenter<RiderDatabaseSettingPanel> = DatabaseDeployViewPresenter()

    // Widgets
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
    private lateinit var lblSqlServerUrlSuffix: JLabel
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

        txtDbName.text = model.databaseName ?: String.format(DEFAULT_SQL_DATABASE_NAME, date)
        txtNewResGrp.text = model.resourceGroupName ?: "$DEFAULT_RESOURCE_GROUP_NAME$date"
        txtNewSqlServerName.text = model.sqlServerName ?: "$DEFAULT_SQL_SERVER_NAME$date"
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
            model.databaseEdition = lastSelectedDatabaseEdition
        } else {
            model.isCreatingSqlServer = false
            val sqlServer = cbExistSqlServer.getItemAt(cbExistSqlServer.selectedIndex)
            if (sqlServer != null) {
                model.sqlServerId = sqlServer.id()
            }
            val adminLogin = lblExistingSqlServerAdminLogin.text
            if (adminLogin != null) {
                model.sqlServerAdminLogin = adminLogin
            }
        }

        // Pricing Tier
        // TODO: ...

        // Collation
        model.collation = txtCollationValue.text
    }

    //endregion Read From Config

    //region MVP View

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
                    if (Comparing.equal(it.name(), configuration.model.resourceGroupName)) {
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
                    if (it.name() ==  configuration.model.sqlServerName) {
                        cbExistSqlServer.selectedItem = it
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

    //region Editing SQL Database Table

    /**
     * Make original components initialization:
     * - database table
     * - ...
     * TODO: Maybe DELETE!!!
     */
    private fun createUIComponents() { }

    //endregion Editing SQL Database Table

    //region Button Groups Behavior

    private fun initButtonGroupsState() {
        initResourceGroupButtonGroup()
        initSqlServerButtonsGroup()
    }

    /**
     * Button groups - Resource Group
     */
    private fun initResourceGroupButtonGroup() {
        val btnGroupResourceGroup = ButtonGroup()
        btnGroupResourceGroup.add(rdoCreateResGrp)
        btnGroupResourceGroup.add(rdoUseExistResGrp)
        rdoCreateResGrp.addActionListener { toggleResourceGroupPanel(true) }
        rdoUseExistResGrp.addActionListener { toggleResourceGroupPanel(false) }
        toggleResourceGroupPanel(false)
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
     */
    private fun initSqlServerButtonsGroup() {
        val btnGroupSqlServer = ButtonGroup()
        btnGroupSqlServer.add(rdoCreateSqlServer)
        btnGroupSqlServer.add(rdoUseExistSqlServer)
        rdoCreateSqlServer.addActionListener { toggleSqlServerPanel(true) }
        rdoUseExistSqlServer.addActionListener { toggleSqlServerPanel(false) }
        toggleSqlServerPanel(false)
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

    //endregion Button Groups Behavior

    //region Configure UI Components

    /**
     * Configure renderer and listeners for all UI Components
     * TODO: SD -- Probably could be merged with {createUIComponents} method above !!!
     */
    private fun setUIComponents() {
        setSubscriptionComboBox()
        setResourceGroupComboBox()
        setDbServerComboBox()
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
                resetResourceGroupComboBoxValues()

                // TODO: Here we should use cached values to not wait until every time until we make a request
                // TODO: to the azure to get the relevant list again and again. Similar to AzureModel
                // TODO: (whether extend or write our own or make this inside [AzureDatabaseMvpModel] ???)
                val subscriptionSid = lastSelectedSubscriptionId
                if (subscriptionSid != null)
                    myView.onLoadSqlServers(subscriptionSid, resourceGroupName)

                lastSelectedResourceGroupName = resourceGroupName
            }
        }
    }

    /**
     * Configure SQL Server combo box
     */
    private fun setDbServerComboBox() {
        cbExistSqlServer.renderer = object : ListCellRendererWrapper<SqlServer>() {
            override fun customize(list: JList<*>, sqlServer: SqlServer?, index: Int, selected: Boolean, hasFocus: Boolean) {
                sqlServer ?: return
                setText(sqlServer.name())
            }
        }

        cbExistSqlServer.addActionListener {
            val sqlServer = cbExistSqlServer.getItemAt(cbExistSqlServer.selectedIndex) ?: return@addActionListener

            if (sqlServer.name() != lastSelectedSqlServer) {
                lblExistingSqlServerLocation.text = sqlServer.region().label()
                lblExistingSqlServerAdminLogin.text = sqlServer.administratorLogin()

                lastSelectedSqlServer = sqlServer.name()
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
            val location = cbLocation.getItemAt(cbLocation.selectedIndex)
            if (location != null) {
                lastSelectedLocation = location.name()
            }
        }
    }

    /**
     * Configure Database Edition combo box
     */
    private fun setDatabaseEditionComboBox() {
        cbDatabaseEdition.addActionListener {
            val databaseEdition = cbDatabaseEdition.getItemAt(cbDatabaseEdition.selectedIndex)
            if (databaseEdition != null) {
                lastSelectedDatabaseEdition = databaseEdition
            }
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
}
