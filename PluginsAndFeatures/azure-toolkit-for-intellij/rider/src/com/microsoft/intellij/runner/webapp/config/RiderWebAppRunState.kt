/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.helpers.UiConstants
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.getAppUrl
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.openAppInBrowser
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.projectAssemblyRelativePath
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.refreshAzureExplorer
import com.microsoft.intellij.runner.database.config.deploy.DatabaseDeployUtil.getOrCreateSqlDatabaseFromConfig
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil.addConnectionString
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil.deployToAzureWebApp
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil.getOrCreateWebAppFromConfiguration
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil.setStartupCommand
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil.webAppStart
import com.microsoft.intellij.runner.webapp.model.DotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.RiderAzureBundle.message

class RiderWebAppRunState(project: Project,
                          private val myModel: DotNetWebAppSettingModel) : AzureRunProfileState<Pair<WebApp, SqlDatabase?>>(project) {

    private var isWebAppCreated = false
    private var isDatabaseCreated = false

    companion object {
        private const val TARGET_NAME = "WebApp"
        private const val URL_WEB_APP_WWWROOT = "/home/site/wwwroot"
    }

    /**
     * Execute a Azure Publish run configuration step. The step creates [WebApp] azure instance with
     * new or existing web app and deploy the selected project to this web app.
     *
     * @param processHandler - a process handler to show a process message
     * @param telemetryMap - a key-value map for collecting telemetry
     *
     * @throws [RuntimeException] exception during step execution
     * @return [WebApp] or null instance with a new azure web app that contains deployed application
     */
    @Throws(RuntimeException::class)
    public override fun executeSteps(processHandler: RunProcessHandler,
                                     telemetryMap: MutableMap<String, String>): Pair<WebApp, SqlDatabase?>? {

        val publishableProject = myModel.webAppModel.publishableProject
                ?: throw RuntimeException(message("process_event.publish.project.not_defined"))

        val subscriptionId = myModel.webAppModel.subscription?.subscriptionId()
                ?: throw RuntimeException(message("process_event.publish.subscription.not_defined"))

        val webApp = getOrCreateWebAppFromConfiguration(myModel.webAppModel, processHandler)
        deployToAzureWebApp(project, publishableProject, webApp, processHandler)

        if (myModel.webAppModel.operatingSystem == OperatingSystem.LINUX && publishableProject.isDotNetCore) {
            val assemblyPath = "$URL_WEB_APP_WWWROOT/$projectAssemblyRelativePath"
            val startupCommand = String.format(UiConstants.WEB_APP_STARTUP_COMMAND_TEMPLATE, assemblyPath)
            setStartupCommand(webApp, startupCommand, myModel.webAppModel.netCoreRuntime, processHandler)
        }

        isWebAppCreated = true

        var database: SqlDatabase? = null

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            database = getOrCreateSqlDatabaseFromConfig(myModel.databaseModel, processHandler)

            val databaseUri = AzureMvpModel.getInstance().getResourceUri(subscriptionId, database.id())
            if (databaseUri != null)
                processHandler.setText(message("process_event.publish.sql_db.url", databaseUri))

            if (myModel.databaseModel.connectionStringName.isEmpty())
                throw RuntimeException(RiderAzureBundle.message("process_event.publish.connection_string.not_defined"))

            if (myModel.databaseModel.sqlServerAdminLogin.isEmpty())
                throw RuntimeException(message("process_event.publish.sql_server.admin_login_not_defined"))

            if (myModel.databaseModel.sqlServerAdminPassword.isEmpty())
                throw RuntimeException(message("process_event.publish.sql_server.admin_password_not_defined"))

            addConnectionString(
                    subscriptionId,
                    webApp,
                    database,
                    myModel.databaseModel.connectionStringName,
                    myModel.databaseModel.sqlServerAdminLogin,
                    myModel.databaseModel.sqlServerAdminPassword,
                    processHandler)
        }

        isDatabaseCreated = true

        webAppStart(webApp, processHandler)

        val url = getAppUrl(webApp)
        processHandler.setText(message("process_event.publish.url", url))
        processHandler.setText(message("process_event.publish.done"))

        return Pair(webApp, database)
    }

    override fun onSuccess(result: Pair<WebApp, SqlDatabase?>, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()

        if (myModel.webAppModel.isCreatingNewApp) {
            refreshAzureExplorer(listenerId = "WebAppModule")
        }

        val (webApp, sqlDatabase) = result
        refreshWebAppAfterPublish(webApp, myModel.webAppModel)

        if (sqlDatabase != null) {
            refreshDatabaseAfterPublish(sqlDatabase, myModel.databaseModel)
        }

        showPublishNotification(message("notification.publish.publish_complete"), NotificationType.INFORMATION)

        val isOpenBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.OPEN_IN_BROWSER_AFTER_PUBLISH_DEFAULT_VALUE)

        if (isOpenBrowser) {
            openAppInBrowser(webApp, processHandler)
        }
    }

    override fun onFail(errMsg: String, processHandler: RunProcessHandler) {
        if (processHandler.isProcessTerminated || processHandler.isProcessTerminating) return

        if (isWebAppCreated)
            AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()

        if (isDatabaseCreated)
            AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()

        showPublishNotification(message("notification.publish.publish_failed"), NotificationType.ERROR)

        processHandler.println(errMsg, ProcessOutputTypes.STDERR)
        processHandler.notifyComplete()
    }

    override fun getDeployTarget() = TARGET_NAME

    private fun refreshWebAppAfterPublish(webApp: WebApp, model: WebAppPublishModel) {
        model.resetOnPublish(webApp)
        AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()
    }

    private fun refreshDatabaseAfterPublish(sqlDatabase: SqlDatabase, model: DatabasePublishModel) {
        model.resetOnPublish(sqlDatabase)
        AzureSqlDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
    }

    private fun showPublishNotification(text: String, type: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("Azure Web App Publish Message")
                .createNotification(text, type)

        Notifications.Bus.notify(notification, project)
    }
}
