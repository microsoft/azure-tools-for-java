package com.microsoft.intellij.runner.webapp.webappconfig

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
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.configuration.AzureRiderSettings
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.DatabaseRunState.getOrCreateSqlDatabaseFromConfig
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.DatabaseRunState.getSqlDatabaseUri
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.addConnectionString
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.deployToAzureWebApp
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.getOrCreateWebAppFromConfiguration
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.getWebAppUrl
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.openWebAppInBrowser
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.setStartupCommand
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.webAppStart
import com.microsoft.intellij.runner.webapp.webappconfig.runstate.WebAppRunState.webAppStop

class RiderWebAppRunState(project: Project,
                          private val myModel: AzureDotNetWebAppSettingModel) : AzureRunProfileState<Pair<WebApp, SqlDatabase?>>(project) {

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
     * @throws [Exception] exception during step execution
     * @return [WebApp] or null instance with a new azure web app that contains deployed application
     */
    @Throws(Exception::class)
    public override fun executeSteps(processHandler: RunProcessHandler,
                                     telemetryMap: MutableMap<String, String>): Pair<WebApp, SqlDatabase?>? {

        val publishableProject = myModel.webAppModel.publishableProject ?: throw RuntimeException(UiConstants.PROJECT_NOT_DEFINED)
        val subscriptionId = myModel.webAppModel.subscription?.subscriptionId() ?: throw RuntimeException(UiConstants.SUBSCRIPTION_NOT_DEFINED)

        val webApp = getOrCreateWebAppFromConfiguration(myModel.webAppModel, processHandler)

        webAppStop(webApp, processHandler)
        deployToAzureWebApp(project, publishableProject, webApp, processHandler)

        if (myModel.webAppModel.operatingSystem == OperatingSystem.LINUX && publishableProject.isDotNetCore) {
            val startupCommand = String.format(UiConstants.WEB_APP_STARTUP_COMMAND_TEMPLATE, "$URL_WEB_APP_WWWROOT/${WebAppRunState.projectAssemblyRelativePath}")
            setStartupCommand(webApp, startupCommand, myModel.webAppModel.netCoreRuntime, processHandler)
        }

        isWebAppCreated = true

        var database: SqlDatabase? = null

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            database = getOrCreateSqlDatabaseFromConfig(myModel.databaseModel, processHandler)

            val databaseUri = getSqlDatabaseUri(subscriptionId, database)
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

        val url = getWebAppUrl(webApp)
        processHandler.setText("URL: $url")
        processHandler.setText(UiConstants.PUBLISH_DONE)

        return Pair(webApp, database)
    }

    override fun onSuccess(result: Pair<WebApp, SqlDatabase?>, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()

        if (myModel.webAppModel.isCreatingWebApp && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
        }

        val webApp = result.first
        refreshWebAppAfterPublish(webApp, myModel.webAppModel)

        val sqlDatabase = result.second
        if (sqlDatabase != null) {
            refreshDatabaseAfterPublish(sqlDatabase, myModel.databaseModel)
        }

        showPublishNotification(TOOL_NOTIFICATION_PUBLISH_SUCCEEDED, NotificationType.INFORMATION)

        val isOpenBrowser = PropertiesComponent.getInstance().getBoolean(
                AzureRiderSettings.PROPERTY_WEB_APP_OPEN_IN_BROWSER_NAME,
                AzureRiderSettings.openInBrowserDefaultValue)

        if (isOpenBrowser)
            openWebAppInBrowser(webApp, processHandler)
    }

    override fun onFail(errMsg: String, processHandler: RunProcessHandler) {
        if (processHandler.isProcessTerminated || processHandler.isProcessTerminating) return

        if (isWebAppCreated)
            AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()

        if (isDatabaseCreated)
            AzureDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()

        showPublishNotification(TOOL_NOTIFICATION_PUBLISH_FAILED, NotificationType.ERROR)

        processHandler.println(errMsg, ProcessOutputTypes.STDERR)
        processHandler.notifyComplete()
    }

    override fun getDeployTarget(): String {
        return TARGET_NAME
    }

    private fun refreshWebAppAfterPublish(webApp: WebApp, model: AzureDotNetWebAppSettingModel.WebAppModel) {
        model.resetOnPublish(webApp)
        AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()
    }

    private fun refreshDatabaseAfterPublish(sqlDatabase: SqlDatabase, model: AzureDotNetWebAppSettingModel.DatabaseModel) {
        model.resetOnPublish(sqlDatabase)
        AzureDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
    }

    private fun showPublishNotification(text: String, type: NotificationType) {
        val displayId = NotificationGroup.toolWindowGroup("Azure Web App Publish Message", ToolWindowId.RUN).displayId
        val notification = Notification(displayId, "", text, type)
        Notifications.Bus.notify(notification, project)
    }
}
