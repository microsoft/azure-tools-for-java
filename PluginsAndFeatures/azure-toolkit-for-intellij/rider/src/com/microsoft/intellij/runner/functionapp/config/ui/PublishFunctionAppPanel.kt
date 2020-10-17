/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.ui

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.update.Activatable
import com.intellij.util.ui.update.UiNotifyConnector
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.azuretools.ijidea.utility.UpdateProgressIndicator
import com.microsoft.azuretools.utils.AzureModelController
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.database.config.ui.DatabasePublishComponent
import com.microsoft.intellij.runner.functionapp.config.FunctionAppConfiguration
import icons.CommonIcons
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JPanel

class PublishFunctionAppPanel(private val lifetime: Lifetime,
                              project: Project,
                              configuration: FunctionAppConfiguration) :
        AzureRiderSettingPanel<FunctionAppConfiguration>(project),
        FunctionAppDeployMvpView,
        Activatable {

    companion object {
        private val webAppIcon = CommonIcons.AzureFunctions.FunctionApp
        private val databaseIcon = CommonIcons.Database
    }

    private val presenter = FunctionAppDeployViewPresenter<PublishFunctionAppPanel>()

    private val pnlRoot = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val tpRoot = JBTabbedPane()
    private val pnlFunctionAppConfigTab = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))
    private val pnlDbConnectionTab = JPanel(MigLayout("novisualpadding, ins 0, fillx, wrap 1"))

    private val pnlFunctionAppConfig =
            FunctionAppPublishComponent(lifetime.createNested(), project, configuration.model.functionAppModel)

    private val pnlDatabaseConnection =
            DatabasePublishComponent(lifetime.createNested(), configuration.model.databaseModel)

    override val panelName = message("run_config.publish.form.function_app.app_settings_panel_name")
    override var mainPanel: JPanel = pnlRoot

    init {
        initSubscriptionAction()
        initRefreshButtonAction()

        pnlFunctionAppConfigTab.apply {
            add(pnlFunctionAppConfig, "growx")
        }

        pnlDbConnectionTab.apply {
            add(pnlDatabaseConnection, "growx")
        }

        tpRoot.apply {
            addTab(message("run_config.publish.form.function_app.app_config_tab_name"), webAppIcon, pnlFunctionAppConfigTab)
            addTab(message("run_config.publish.form.function_app.db_config_tab_name"), databaseIcon, pnlDbConnectionTab)
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
     * @param configuration - Function App Configuration instance
     */
    override fun resetFromConfig(configuration: FunctionAppConfiguration) {
        val dateString = SimpleDateFormat("yyMMddHHmmss").format(Date())

        pnlFunctionAppConfig.resetFromConfig(configuration.model.functionAppModel, dateString)
        pnlDatabaseConnection.resetFromConfig(configuration.model.databaseModel, dateString)
    }

    /**
     * Function is triggered by any content change events.
     *
     * @param configuration configuration instance
     */
    override fun apply(configuration: FunctionAppConfiguration) {
        pnlFunctionAppConfig.applyConfig(configuration.model.functionAppModel)
        pnlDatabaseConnection.applyConfig(configuration.model.databaseModel)
    }

    //endregion Read From Config

    override fun disposeEditor() {
        presenter.onDetachView()
    }

    override fun fillFunctionAppsTable(apps: List<ResourceEx<FunctionApp>>) {
        pnlFunctionAppConfig.fillAppsTable(apps)
        collectConnectionStringsInBackground(apps.map { it.resource })
    }

    override fun fillSubscription(subscriptions: List<Subscription>) {
        pnlFunctionAppConfig.fillSubscription(subscriptions)
    }

    override fun fillResourceGroup(resourceGroups: List<ResourceGroup>) {
        pnlFunctionAppConfig.fillResourceGroup(resourceGroups)
        pnlDatabaseConnection.fillResourceGroup(resourceGroups)
    }

    override fun fillAppServicePlan(appServicePlans: List<AppServicePlan>) {
        pnlFunctionAppConfig.fillAppServicePlan(appServicePlans)
    }

    override fun fillLocation(locations: List<Location>) {
        pnlFunctionAppConfig.fillLocation(locations)
        pnlDatabaseConnection.fillLocation(locations)
    }

    override fun fillPricingTier(prices: List<PricingTier>) {
        pnlFunctionAppConfig.fillPricingTier(prices)
    }

    override fun fillSqlDatabase(databases: List<SqlDatabase>) {
        pnlDatabaseConnection.fillSqlDatabase(databases)
    }

    override fun fillSqlServer(sqlServers: List<SqlServer>) {
        pnlDatabaseConnection.fillSqlServer(sqlServers)
    }

    override fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>) {
        pnlFunctionAppConfig.fillPublishableProject(publishableProjects)
    }

    override fun fillStorageAccount(storageAccount: List<StorageAccount>) {
        pnlFunctionAppConfig.fillStorageAccount(storageAccount)
    }

    override fun fillStorageAccountType(storageAccountType: List<StorageAccountSkuType>) {
        pnlFunctionAppConfig.fillStorageAccountType(storageAccountType)
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
        presenter.onLoadStorageAccountTypes(lifetime)
    }

    override fun hideNotify() {}

    private fun initSubscriptionAction() {
        pnlFunctionAppConfig.pnlCreateFunctionApp.pnlSubscription.listenerAction = { subscription ->
            val selectedSid = subscription.subscriptionId()

            presenter.onLoadResourceGroups(lifetime, selectedSid)
            presenter.onLoadLocation(lifetime, selectedSid)
            presenter.onLoadAppServicePlan(lifetime, selectedSid)
            presenter.onLoadStorageAccounts(lifetime, selectedSid)

            pnlDatabaseConnection.subscription = subscription
            presenter.onLoadSqlServers(lifetime, selectedSid)
            presenter.onLoadSqlDatabase(lifetime, selectedSid)
        }
    }

    private fun initRefreshButtonAction() {
        pnlFunctionAppConfig.pnlExistingFunctionApp.pnlExistingAppTable.tableRefreshAction = {
            presenter.onRefresh(lifetime)
        }
    }

    //region Azure Model

    /**
     * Update cached values in [com.microsoft.azuretools.utils.AzureModel] and [AzureDatabaseMvpModel]
     * in the background to use for fields validation on client side
     */
    private fun updateAzureModelInBackground() {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, message("progress.publish.function_app.updating_azure_model"), false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                AzureModelController.updateSubscriptionMaps(UpdateProgressIndicator(indicator))
                AzureModelController.updateResourceGroupMaps(UpdateProgressIndicator(indicator))
                AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
            }
        })
    }

    private fun collectConnectionStringsInBackground(apps: List<FunctionApp>) {
        if (apps.isEmpty()) return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, message("progress.publish.function_app.updating_connection_strings_map"), false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                apps.forEach { functionApp -> AzureFunctionAppMvpModel.getConnectionStrings(functionApp, true) }
            }
        })
    }

    //endregion Azure Model
}