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

package com.microsoft.intellij.runner.webapp.config

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowId
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.helpers.UiConstants
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.getAppUrl
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.openAppInBrowser
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.projectAssemblyRelativePath
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

class RiderWebAppRunState(project: Project,
                          private val myModel: DotNetWebAppSettingModel) : AzureRunProfileState<Pair<WebApp, SqlDatabase?>>(project) {

    private var isWebAppCreated = false
    private var isDatabaseCreated = false

    companion object {
        private const val TARGET_NAME = "WebApp"
        private const val URL_WEB_APP_WWWROOT = "/home/site/wwwroot"
        private const val TOOL_NOTIFICATION_PUBLISH_SUCCEEDED = "Azure Publish completed"
        private const val TOOL_NOTIFICATION_PUBLISH_FAILED = "Azure Publish failed"
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

        val publishableProject = myModel.webAppModel.publishableProject ?: throw RuntimeException(UiConstants.PROJECT_NOT_DEFINED)
        val subscriptionId = myModel.webAppModel.subscription?.subscriptionId() ?: throw RuntimeException(UiConstants.SUBSCRIPTION_NOT_DEFINED)

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
                processHandler.setText(String.format(UiConstants.SQL_DATABASE_URL, databaseUri))

            if (myModel.databaseModel.connectionStringName.isEmpty()) throw RuntimeException(UiConstants.CONNECTION_STRING_NAME_NOT_DEFINED)
            if (myModel.databaseModel.sqlServerAdminLogin.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            if (myModel.databaseModel.sqlServerAdminPassword.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

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
        processHandler.setText("URL: $url")
        processHandler.setText(UiConstants.PUBLISH_DONE)

        return Pair(webApp, database)
    }

    override fun onSuccess(result: Pair<WebApp, SqlDatabase?>, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()

        if (myModel.webAppModel.isCreatingNewApp && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
        }

        val (webApp, sqlDatabase) = result
        refreshWebAppAfterPublish(webApp, myModel.webAppModel)

        if (sqlDatabase != null) {
            refreshDatabaseAfterPublish(sqlDatabase, myModel.databaseModel)
        }

        showPublishNotification(TOOL_NOTIFICATION_PUBLISH_SUCCEEDED, NotificationType.INFORMATION)

        val isOpenBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.openInBrowserDefaultValue)

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

        showPublishNotification(TOOL_NOTIFICATION_PUBLISH_FAILED, NotificationType.ERROR)

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
        val displayId = NotificationGroup.toolWindowGroup("Azure Web App Publish Message", ToolWindowId.RUN).displayId
        val notification = Notification(displayId, "", text, type)
        Notifications.Bus.notify(notification, project)
    }
}
