/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config.ui

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.runner.database.config.ui.DatabasePublishComponent
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.config.RiderWebAppConfiguration
import net.miginfocom.swing.MigLayout
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JPanel

class WebAppPublishPanel(private val lifetime: Lifetime,
                         project: Project,
                         configuration: RiderWebAppConfiguration) :
        AzureRiderSettingPanel<RiderWebAppConfiguration>(project),
        DotNetWebAppDeployMvpView,
        Activatable {

    companion object {
        private const val WEB_APP_SETTINGS_PANEL_NAME = "Run On Web App"

        private const val RUN_CONFIG_PREFIX = "Azure Web App"

        private const val TAB_WEB_APP_CONFIGURATION = "Web App Configuration"
        private const val TAB_DATABASE_CONNECTION = "Database Connection"

        private val webAppIcon = IconLoader.getIcon("icons/WebApp.svg")
        private val databaseIcon = IconLoader.getIcon("icons/Database.svg")
    }

    private val presenter = DotNetWebAppDeployViewPresenter<WebAppPublishPanel>()

    private val pnlRoot = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val tpRoot = JBTabbedPane()
    private val pnlWebAppConfigTab = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    private val pnlDbConnectionTab = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val pnlWebAppConfig =
            WebAppPublishComponent(lifetime.createNested(), project, configuration.model.webAppModel)

    private val pnlDatabaseConnection =
            DatabasePublishComponent(lifetime.createNested(), configuration.model.databaseModel)

    // Panels
    override val panelName = WEB_APP_SETTINGS_PANEL_NAME
    override var mainPanel: JPanel = pnlRoot

    init {
        initSubscriptionAction()
        initRefreshButtonAction()

        pnlWebAppConfigTab.apply {
            add(pnlWebAppConfig, "growx")
        }

        pnlDbConnectionTab.apply {
            add(pnlDatabaseConnection, "growx")
        }

        tpRoot.apply {
            addTab(TAB_WEB_APP_CONFIGURATION, webAppIcon, pnlWebAppConfigTab)
            addTab(TAB_DATABASE_CONNECTION, databaseIcon, pnlDbConnectionTab)
        }

        pnlRoot.apply {
            add(tpRoot, "growx")
        }

        UiNotifyConnector.Once(mainPanel, this)

        presenter.onAttachView(this)

        updateAzureModelInBackground()
    }

    //region Read From Config

    /**
     * Reset all controls from configuration.
     * Function is triggered while constructing the panel.
     *
     * @param configuration - Web App Configuration instance
     */
    override fun resetFromConfig(configuration: RiderWebAppConfiguration) {
        val dateString = SimpleDateFormat("yyMMddHHmmss").format(Date())

        pnlWebAppConfig.resetFromConfig(configuration.model.webAppModel, dateString)
        pnlDatabaseConnection.resetFromConfig(configuration.model.databaseModel, dateString)
    }

    /**
     * Function is triggered by any content change events.
     *
     * @param configuration configuration instance
     */
    override fun apply(configuration: RiderWebAppConfiguration) {
        pnlWebAppConfig.applyConfig(configuration.model.webAppModel)
        pnlDatabaseConnection.applyConfig(configuration.model.databaseModel)
    }

    //endregion Read From Config

    override fun disposeEditor() {
        presenter.onDetachView()
    }

    override fun fillExistingWebAppsTable(webApps: List<ResourceEx<WebApp>>) {
        pnlWebAppConfig.fillAppsTable(webApps)
        collectConnectionStringsInBackground(webApps.map { it.resource })
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        pnlWebAppConfig.fillSubscription(subscriptions)
    }

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        pnlWebAppConfig.fillResourceGroup(resourceGroups)
        pnlDatabaseConnection.fillResourceGroup(resourceGroups)
    }

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {
        pnlWebAppConfig.fillAppServicePlan(appServicePlans)
    }

    override fun fillLocation(locations: List<Location>) {
        pnlWebAppConfig.fillLocation(locations)
        pnlDatabaseConnection.fillLocation(locations)
    }

    override fun fillPricingTier(prices: List<PricingTier>) {
        pnlWebAppConfig.fillPricingTier(prices)
    }

    override fun fillSqlDatabase(databases: List<SqlDatabase>) {
        pnlDatabaseConnection.fillSqlDatabase(databases)
    }

    override fun fillSqlServer(sqlServers: List<SqlServer>) {
        pnlDatabaseConnection.fillSqlServer(sqlServers)
    }

    override fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlWebAppConfig.fillPublishableProject(publishableProjects)
    }

    /**
     * Execute on showing the form to make sure we run with a correct modality state to properly pull the UI thread
     *
     * Note: There are two different ways to launch the publish editor: from run config and from context menu.
     *       In case we run from a run config, we have a run configuration modal window, while context menu set a correct modality only
     *       when an editor is shown up. We need to wait for a window to show to get a correct modality state for a publish editor
     */
    override fun showNotify() {
        presenter.onLoadPublishableProjects(lifetime, project)
        presenter.onLoadSubscription(lifetime)
        presenter.onLoadApps(lifetime)
        presenter.onLoadPricingTier(lifetime)
    }

    override fun hideNotify() {}

    private fun initSubscriptionAction() {
        pnlWebAppConfig.pnlCreateWebApp.pnlSubscription.listenerAction = { subscription ->
            val selectedSid = subscription.subscriptionId()

            presenter.onLoadResourceGroups(lifetime, selectedSid)
            presenter.onLoadLocation(lifetime, selectedSid)
            presenter.onLoadAppServicePlan(lifetime, selectedSid)

            pnlDatabaseConnection.subscription = subscription
            presenter.onLoadSqlServers(lifetime, selectedSid)
            presenter.onLoadSqlDatabase(lifetime, selectedSid)
        }
    }

    private fun initRefreshButtonAction() {
        pnlWebAppConfig.pnlExistingWebApp.pnlExistingAppTable.tableRefreshAction = {
            presenter.onRefresh(lifetime)
        }
    }

    //region Azure Model

    /**
     * Update cached values in [com.microsoft.azuretools.utils.AzureModel] and [AzureSqlDatabaseMvpModel]
     * in the background to use for fields validation on client side
     */
    private fun updateAzureModelInBackground() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Azure model", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureModelController.updateResourceGroupMaps(UpdateProgressIndicator(indicator))
                AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
            }
        })
    }

    private fun collectConnectionStringsInBackground(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Updating Web App connection strings map", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                webApps.forEach { webApp -> AzureDotNetWebAppMvpModel.getConnectionStrings(webApp, true) }
            }
        })
    }

    //endregion Azure Model
}
