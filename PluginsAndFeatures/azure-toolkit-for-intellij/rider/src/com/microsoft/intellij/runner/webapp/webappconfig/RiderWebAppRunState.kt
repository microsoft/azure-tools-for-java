package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.run.configurations.publishing.base.MsBuildPublishingService
import com.jetbrains.rider.util.idea.application
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.utils.WebAppDeploySession
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import okhttp3.Response
import org.jetbrains.concurrency.AsyncPromise
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.zip.ZipOutputStream

class RiderWebAppRunState(project: Project,
                          private val myModel: AzureDotNetWebAppSettingModel) : AzureRunProfileState<Pair<WebApp, SqlDatabase?>>(project) {

    companion object {
        private const val TARGET_NAME = "WebApp"

        private const val PUBLISH_DONE = "Done."

        // Subscription
        private const val SUBSCRIPTION_ID_NOT_DEFINED = "Subscription ID is not defined"

        private const val PROJECT_NOT_DEFINED = "Project is not defined"

        private const val WEB_APP_START = "Start Web App '%s'..."
        private const val WEB_APP_STOP = "Stop Web App '%s'..."
        private const val WEB_APP_CREATE = "Creating Web App '%s'..."
        private const val WEB_APP_CREATE_SUCCESSFUL = "Web App  is created, id: '%s'"
        private const val WEB_APP_GET_EXISTING = "Got existing Web App with Id: '%s'"
        private const val WEB_APP_ID_NOT_DEFINED = "Web App ID is not defined"
        private const val WEB_APP_NAME_NOT_DEFINED = "Web App Name is not defined"
        private const val WEB_APP_SET_STARTUP_FILE = "Set Startup File for a web app: '%s'"
        private const val WEB_APP_STARTUP_COMMAND_TEMPLATE = "dotnet %s"
        private const val WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME = "DOCKER_CUSTOM_IMAGE_NAME"

        private const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group Name is not defined"

        private const val APP_SERVICE_PLAN_ID_NOT_DEFINED = "App Service Plan ID is not defined"
        private const val APP_SERVICE_PLAN_NAME_NOT_DEFINED = "App Service Plan Name is not defined"
        private const val APP_SERVICE_PLAN_LOCATION_NOT_DEFINED = "App Service Plan Location is not defined"

        private const val DEPLOY_SUCCESSFUL = "Deploy succeeded."

        private const val SQL_DATABASE_NOT_DEFINED = "Database not set"
        private const val SQL_DATABASE_GET_EXISTING = "Got existing SQL Database: %s"
        private const val SQL_DATABASE_CREATE = "Creating SQL Database '%s'..."
        private const val SQL_DATABASE_CREATE_SUCCESSFUL = "SQL Database is created successfully."
        private const val SQL_DATABASE_NAME_NOT_DEFINED = "SQL Database Name is not defined"
        private const val SQL_DATABASE_URL = "Please see SQL Database details by URL: %s"

        private const val SQL_SERVER_CREATE = "Creating SQL Server '%s'..."
        private const val SQL_SERVER_GET_EXISTING = "Get existing SQL Server with Id: '%s'"
        private const val SQL_SERVER_CREATE_SUCCESSFUL = "SQL Server is created, id: '%s'"
        private const val SQL_SERVER_NAME_NOT_DEFINED = "SQL Server Name is not defined"
        private const val SQL_SERVER_REGION_NOT_DEFINED = "SQL Server Region is not defined"
        private const val SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED = "SQL Server Resource Group Name is not defined"
        private const val SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED = "SQL Server Admin Login is not defined"
        private const val SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED = "SQL Server Admin Password is not defined"
        private const val SQL_SERVER_ID_NOT_DEFINED = "SQL Server ID is not defined"
        private const val SQL_SERVER_CANNOT_GET = "Unable to find SQL Server with name '%s'"

        private const val CONNECTION_STRING_NAME_NOT_DEFINED = "Connection string not set"
        private const val CONNECTION_STRING_CREATING = "Creating connection string with name '%s'..."
        private const val CONNECTION_STRING_CREATE_FAILED = "Failed to create Connection String to web app: %s"

        private const val PROJECT_ARTIFACTS_COLLECTING = "Collecting '%s' project artifacts..."
        private const val PROJECT_ARTIFACTS_COLLECTING_FAILED = "Failed collecting project artifacts. Please see Build output"

        private const val ZIP_FILE_CREATE_FOR_PROJECT = "Creating '%s' project ZIP..."
        private const val ZIP_FILE_CREATE_SUCCESSFUL = "Project ZIP is created: '%s'"
        private const val ZIP_FILE_NOT_CREATED = "Unable to create a ZIP file"
        private const val ZIP_FILE_DELETING = "Deleting ZIP file '%s'"
        private const val ZIP_DEPLOY_START_PUBLISHING = "Publishing ZIP file. Attempt %s of %s..."
        private const val ZIP_DEPLOY_PUBLISH_SUCCESS = "Published ZIP file successfully"
        private const val ZIP_DEPLOY_PUBLISH_FAIL = "Fail publishing ZIP file: %s"

        private const val URL_AZURE_BASE = ".azurewebsites.net"
        private const val URL_KUDU_BASE = ".scm$URL_AZURE_BASE"
        private const val URL_KUDU_ZIP_DEPLOY = "$URL_KUDU_BASE/api/zipdeploy"
        private const val URL_WEB_APP_WWWROOT = "/home/site/wwwroot"

        private const val COLLECT_ARTIFACTS_TIMEOUT_MS = 180000L
        private const val DEPLOY_TIMEOUT_MS = 180000L
        private const val SLEEP_TIME_MS = 5000L
        private const val UPLOADING_MAX_TRY = 3
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

        val publishableProject = myModel.publishableProject ?: throw Exception(PROJECT_NOT_DEFINED)

        if (myModel.subscriptionId.isEmpty()) throw Exception(SUBSCRIPTION_ID_NOT_DEFINED)

        val webApp = getOrCreateWebAppFromConfiguration(myModel, processHandler)

        webAppStop(webApp, processHandler)
        deployToAzureWebApp(publishableProject, webApp, processHandler)
        if (myModel.operatingSystem == OperatingSystem.LINUX && publishableProject.isDotNetCore) {
            setStartupCommand(webApp, publishableProject.projectName, processHandler)
        }

        var database: SqlDatabase? = null

        if (myModel.isDatabaseConnectionEnabled) {

            database = getOrCreateSqlDatabaseFromConfig(myModel, processHandler)

            val subscriptionId = myModel.subscriptionId
            val databaseUri = getSqlDatabaseUri(subscriptionId, database)
            if (databaseUri != null)
                processHandler.setText(String.format(SQL_DATABASE_URL, databaseUri))

            if (myModel.connectionStringName.isEmpty()) throw Exception(CONNECTION_STRING_NAME_NOT_DEFINED)
            if (myModel.sqlServerAdminLogin.isEmpty()) throw Exception(SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            if (myModel.sqlServerAdminPassword.isEmpty()) throw Exception(SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

            addConnectionString(
                    myModel.subscriptionId,
                    webApp,
                    database,
                    myModel.connectionStringName,
                    myModel.sqlServerAdminLogin,
                    myModel.sqlServerAdminPassword,
                    processHandler)
        }

        webAppStart(webApp, processHandler)

        val url = getWebAppUrl(webApp)
        processHandler.setText("URL: $url")
        processHandler.setText(PUBLISH_DONE)

        return Pair(webApp, database)
    }

    override fun onSuccess(result: Pair<WebApp, SqlDatabase?>, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()
        if (myModel.isCreatingWebApp && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
        }

        refreshWebAppAfterPublish(result.first, myModel)
        val sqlDatabase = result.second
        if (sqlDatabase != null) {
            refreshDatabaseAfterPublish(sqlDatabase, myModel)
        }
    }

    override fun onFail(errMsg: String, processHandler: RunProcessHandler) {
        processHandler.println(errMsg, ProcessOutputTypes.STDERR)
        processHandler.notifyComplete()
    }

    override fun getDeployTarget(): String {
        return TARGET_NAME
    }

    //region Web App

    private fun webAppStart(webApp: WebApp, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(WEB_APP_START, webApp.name()))
        webApp.start()
    }

    private fun webAppStop(webApp: WebApp, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(WEB_APP_STOP, webApp.name()))
        webApp.stop()
    }

    /**
     * Create a web app from a [WebAppSettingModel] instance
     *
     * @param processHandler - a process handler to show a process message
     *
     * @return [WebApp] new or existing web app instance
     */
    private fun getOrCreateWebAppFromConfiguration(model: AzureDotNetWebAppSettingModel,
                                                   processHandler: RunProcessHandler): WebApp {

        if (model.isCreatingWebApp) {
            processHandler.setText(String.format(WEB_APP_CREATE, myModel.webAppName))

            if (model.webAppName.isEmpty()) throw Exception(WEB_APP_NAME_NOT_DEFINED)
            if (model.resourceGroupName.isEmpty()) throw Exception(RESOURCE_GROUP_NAME_NOT_DEFINED)
            val operatingSystem = myModel.operatingSystem

            val webAppDefinition = AzureDotNetWebAppMvpModel.WebAppDefinition(
                    model.webAppName, model.isCreatingResourceGroup, model.resourceGroupName)

            val webApp =
                    if (model.isCreatingAppServicePlan) {
                        if (model.appServicePlanName.isEmpty()) throw Exception(APP_SERVICE_PLAN_NAME_NOT_DEFINED)
                        if (model.location.isEmpty()) throw Exception(APP_SERVICE_PLAN_LOCATION_NOT_DEFINED)
                        val pricingTier = myModel.pricingTier
                        val appServicePlanDefinition = AzureDotNetWebAppMvpModel.AppServicePlanDefinition(model.appServicePlanName, pricingTier, model.location)

                        if (operatingSystem == OperatingSystem.WINDOWS) {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewWindowsAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    myModel.netFrameworkVersion)
                        } else {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewLinuxAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    myModel.netCoreRuntime)
                        }
                    } else {
                        if (model.appServicePlanId.isEmpty()) throw Exception(APP_SERVICE_PLAN_ID_NOT_DEFINED)

                        if (operatingSystem == OperatingSystem.WINDOWS) {
                            AzureDotNetWebAppMvpModel.createWebAppWithExistingWindowsAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    model.appServicePlanId,
                                    model.netFrameworkVersion)
                        } else {
                            AzureDotNetWebAppMvpModel.createWebAppWithExistingLinuxAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    model.appServicePlanId,
                                    myModel.netCoreRuntime)
                        }
                    }

            processHandler.setText(String.format(WEB_APP_CREATE_SUCCESSFUL, webApp.id()))
            return webApp
        }

        processHandler.setText(String.format(WEB_APP_GET_EXISTING, model.webAppId))

        if (model.webAppId.isEmpty()) throw Exception(WEB_APP_ID_NOT_DEFINED)
        return AzureWebAppMvpModel.getInstance().getWebAppById(model.subscriptionId, model.webAppId)
    }

    /**
     * Update a created web app with a connection string
     *
     * @param webApp to set connection string for
     * @param name connection string name
     * @param value connection string value
     * @param processHandler a process handler to show a process message
     */
    private fun updateWithConnectionString(webApp: WebApp, name: String, value: String, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(CONNECTION_STRING_CREATING, name))
        webApp.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()
    }

    /**
     * Get an active URL for web app
     *
     * @param webApp - a web app instance to get URL for
     *
     * @return [String] URL string
     */
    private fun getWebAppUrl(webApp: WebApp): String {
        return "https://" + webApp.defaultHostName()
    }

    /**
     * Set Startup File for a web app
     *
     * Note: Use this method on Azure Linux instances to set up a Startup File command.
     *       SDK support Startup command for public and private Docker images. So, we access the setting through the
     *       [WithPublicDockerHubImage] API. Then we roll back to use a build-in images.
     *       [WithPublicDockerHubImage] API produce an extra App Setting "DOCKER_CUSTOM_IMAGE_NAME".
     *       So, we need to delete it afterwords
     *
     * @param webApp web app instance to update with Startup File
     * @param projectName published project name
     * @param processHandler a process handler to show a process message
     */
    private fun setStartupCommand(webApp: WebApp,
                                  projectName: String,
                                  processHandler: RunProcessHandler) {
        processHandler.setText(String.format(WEB_APP_SET_STARTUP_FILE, webApp.name()))
        webApp.update()
                .withPublicDockerHubImage("") // Hack to access .withStartUpCommand() API
                .withStartUpCommand(String.format(WEB_APP_STARTUP_COMMAND_TEMPLATE, "$URL_WEB_APP_WWWROOT/$projectName.dll"))
                .withBuiltInImage(myModel.netCoreRuntime)
                .apply()

        webApp.update().withoutAppSetting(WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME).apply()
    }

    //endregion Web App

    //region SQL Database

    private fun getOrCreateSqlDatabaseFromConfig(model: AzureDotNetWebAppSettingModel,
                                                 processHandler: RunProcessHandler): SqlDatabase {
        if (model.isCreatingSqlDatabase) {
            val sqlServer = getOrCreateSqlServerFromConfiguration(myModel, processHandler)
            return createDatabase(sqlServer, myModel, processHandler)
        }

        val database = model.database ?: throw Exception(SQL_DATABASE_NOT_DEFINED)

        processHandler.setText(String.format(SQL_DATABASE_GET_EXISTING, database.name()))
        return database
    }

    /**
     * Create a SQL database from a [AzureDotNetWebAppSettingModel] instance
     *
     * @param sqlServer instance of Azure SQL Server that will host a database
     * @param model database model
     * @param processHandler a process handler to show a process message
     *
     * @return [SqlDatabase] instance of a new or existing Azure SQL Database
     */
    private fun createDatabase(sqlServer: SqlServer,
                               model: AzureDotNetWebAppSettingModel,
                               processHandler: RunProcessHandler): SqlDatabase {

        processHandler.setText(String.format(SQL_DATABASE_CREATE, model.databaseName))

        if (model.databaseName.isEmpty()) throw Exception(SQL_DATABASE_NAME_NOT_DEFINED)
        val database = AzureDatabaseMvpModel.createSqlDatabase(sqlServer, model.databaseName, model.collation)

        processHandler.setText(String.format(SQL_DATABASE_CREATE_SUCCESSFUL, database.id()))
        return database
    }

    /**
     * Get database URI from a published SQL Database
     *
     * @param database published database instance
     * @return [java.net.URI] to a SQL Database on Azure portal
     */
    private fun getSqlDatabaseUri(subscriptionId: String, database: SqlDatabase): URI? {
        val azureManager = AuthMethodManager.getInstance().azureManager
        val portalUrl = azureManager.portalUrl

        // Note: [SubscriptionManager.getSubscriptionTenant()] method does not update Subscription to TenantId map while
        //       [SubscriptionManager.getSubscriptionDetails()] force to update and get the correct value
        val tenantId = azureManager.subscriptionManager.subscriptionDetails
                .find { it.subscriptionId == subscriptionId }?.tenantId ?: return null

        val path = "/#@$tenantId/resource/${database.id()}/overview".replace("/+".toRegex(), "/")
        return URI.create("$portalUrl/$path").normalize()
    }

    //endregion SQL Database

    //region SQL Server

    /**
     * Get or create an Azure SQL server based on database model [AzureDotNetWebAppSettingModel] parameters
     *
     * @param model database model
     * @param processHandler a process handler to show a process message
     *
     * @return [SqlServer] instance of existing or a new Azure SQL Server
     */
    private fun getOrCreateSqlServerFromConfiguration(model: AzureDotNetWebAppSettingModel,
                                                      processHandler: RunProcessHandler): SqlServer {

        if (model.subscriptionId.isEmpty()) throw Exception(SUBSCRIPTION_ID_NOT_DEFINED)

        if (model.isCreatingSqlServer) {
            processHandler.setText(String.format(SQL_SERVER_CREATE, model.sqlServerName))

            if (model.sqlServerName.isEmpty()) throw Exception(SQL_SERVER_NAME_NOT_DEFINED)
            if (model.location.isEmpty()) throw Exception(SQL_SERVER_REGION_NOT_DEFINED)
            if (model.dbResourceGroupName.isEmpty()) throw Exception(SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED)
            if (model.sqlServerAdminLogin.isEmpty()) throw Exception(SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            if (model.sqlServerAdminPassword.isEmpty()) throw Exception(SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

            val sqlServer = AzureDatabaseMvpModel.createSqlServer(
                    model.subscriptionId,
                    model.sqlServerName,
                    model.location,
                    model.isCreatingDbResourceGroup,
                    model.dbResourceGroupName,
                    model.sqlServerAdminLogin,
                    model.sqlServerAdminPassword)

            processHandler.setText(String.format(SQL_SERVER_CREATE_SUCCESSFUL, sqlServer.id()))

            return sqlServer
        }

        processHandler.setText(String.format(SQL_SERVER_GET_EXISTING, model.sqlServerId))

        if (model.sqlServerId.isEmpty()) throw Exception(SQL_SERVER_ID_NOT_DEFINED)
        return AzureDatabaseMvpModel.getSqlServerById(model.subscriptionId, model.sqlServerId)
    }

    //endregion SQL Server

    //region Database Connection

    private fun addConnectionString(subscriptionId: String,
                                    webApp: WebApp,
                                    database: SqlDatabase,
                                    connectionStringName: String,
                                    adminLogin: String,
                                    adminPassword: CharArray,
                                    processHandler: RunProcessHandler) {

        val sqlServer = AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, database.sqlServerName(), true)

        if (sqlServer == null) {
            val message = String.format(SQL_SERVER_CANNOT_GET, database.sqlServerName())
            processHandler.setText(String.format(CONNECTION_STRING_CREATE_FAILED, message))
            return
        }

        val fullyQualifiedDomainName = sqlServer.fullyQualifiedDomainName()

        val connectionStringValue =
                "Data Source=tcp:$fullyQualifiedDomainName,1433;" +
                        "Initial Catalog=${database.name()};" +
                        "User Id=$adminLogin@${database.sqlServerName()};" +
                        "Password=${adminPassword.joinToString("")}"

        updateWithConnectionString(webApp, connectionStringName, connectionStringValue, processHandler)
    }

    //endregion Database Connection

    //region Deploy

    /**
     * Deploy an artifacts to a created web app
     *
     * @param webApp - Web App instance to use for deployment
     * @param processHandler - a process handler to show a process message
     */
    private fun deployToAzureWebApp(publishableProject: PublishableProjectModel,
                                    webApp: WebApp,
                                    processHandler: RunProcessHandler) {

        zipDeploy(publishableProject, webApp, processHandler)
        processHandler.setText(DEPLOY_SUCCESSFUL)
    }

    /**
     * Deploy to Web App using KUDU Zip-Deploy
     *
     * @param publishableProject - a project to publish
     * @param webApp - web app to deploy
     * @param processHandler - a process handler to show a process message
     */
    private fun zipDeploy(publishableProject: PublishableProjectModel,
                          webApp: WebApp,
                          processHandler: RunProcessHandler) {

        try {
            processHandler.setText(String.format(PROJECT_ARTIFACTS_COLLECTING, publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)

            processHandler.setText(String.format(ZIP_FILE_CREATE_FOR_PROJECT, publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            webApp.kuduZipDeploy(zipFile, processHandler)

            if (zipFile.exists()) {
                processHandler.setText(String.format(ZIP_FILE_DELETING, zipFile.path))
                FileUtil.delete(zipFile)
            }
        } catch (e: Throwable) {
            processHandler.setText(String.format(ZIP_DEPLOY_PUBLISH_FAIL, e))
            throw e
        }
    }

    /**
     * Collect a selected project artifacts and get a [File] with out folder
     * Note: For a DotNET Framework projects publish a copy of project folder as is
     *       For a DotNet Core projects generate an publishable output by [MsBuildPublishingService]
     *
     * @param project IDEA [Project] instance
     * @param publishableProject contains information about project to be published (isDotNetCore, path to project file)
     *
     * @return [File] to project content to be published
     */
    private fun collectProjectArtifacts(project: Project,
                                        publishableProject: PublishableProjectModel): File {

        // Get out parameters
        val publishService = MsBuildPublishingService.getInstance(project)
        val (targetProperties, outPath) = publishService.getPublishToTempDirParameterAndPath()

        val event = AsyncPromise<BuildResultKind>()
        application.invokeLater {

            val onFinish: (result: BuildResultKind) -> Unit = {
                if (it == BuildResultKind.Successful || it == BuildResultKind.HasWarnings) {
                    requestRunWindowFocus()
                }

                event.setResult(it)
            }

            if (publishableProject.isDotNetCore) {
                publishService.invokeMsBuild(publishableProject.projectFilePath, listOf(targetProperties), false, false, onFinish)
            } else {
                publishService.webPublishToFileSystem(publishableProject.projectFilePath, outPath, false, false, onFinish)
            }
        }

        val buildResult = event.get(COLLECT_ARTIFACTS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (buildResult != BuildResultKind.Successful && buildResult != BuildResultKind.HasWarnings) {
            throw Exception(PROJECT_ARTIFACTS_COLLECTING_FAILED)
        }

        return outPath.toFile().canonicalFile
    }

    /**
     * Archive a directory
     *
     * @param fromFile - file to be added to ZIP archive
     * @param processHandler - a process handler to show a process message
     * @param deleteOriginal - delete original file
     *
     * @return [File] Zip archive file
     */
    @Throws(FileNotFoundException::class)
    private fun zipProjectArtifacts(fromFile: File,
                                    processHandler: RunProcessHandler,
                                    deleteOriginal: Boolean = true): File {

        if (!fromFile.exists()) throw FileNotFoundException("Original file '${fromFile.path}' not found")

        try {
            val toZip = FileUtil.createTempFile(fromFile.nameWithoutExtension, ".zip", true)
            packToZip(fromFile, toZip)
            processHandler.setText(String.format(ZIP_FILE_CREATE_SUCCESSFUL, toZip.path))

            if (deleteOriginal)
                FileUtil.delete(fromFile)

            return toZip
        } catch (e: Throwable) {
            processHandler.setText("$ZIP_FILE_NOT_CREATED: $e")
            throw e
        }
    }

    /**
     * Pack web app to ZIP file
     *
     * @param fileToZip - file that need to be zipped
     * @param zipFileToCreate - zip file that will be created as a result
     * @param filter - filter for zip package
     *
     * @return {File} ZIP file instance
     * @throws [FileNotFoundException] when ZIP file does not exists
     */
    @Throws(FileNotFoundException::class)
    private fun packToZip(fileToZip: File,
                          zipFileToCreate: File,
                          filter: FileFilter? = null) {
        if (!fileToZip.exists()) {
            throw FileNotFoundException("Source file or directory '${fileToZip.path}' does not exist")
        }

        ZipOutputStream(FileOutputStream(zipFileToCreate)).use { zipOutput ->
            ZipUtil.addDirToZipRecursively(zipOutput, null, fileToZip, "", filter, null)
        }
    }

    /**
     * Method to publish specified ZIP file to Azure server. We make up to 3 tries for uploading a ZIP file.
     *
     * Note: Azure SDK support a native [WebApp.zipDeploy(File)] method. Hoverer, we cannot use it for files with BOM
     *       Method throws an exception while reading the JSON file that contains BOM. Use our own implementation until fixed
     *
     * @param zipFile - zip file instance to be published
     * @param processHandler - a process handler to show a process message
     *
     * @throws [Exception] in case REST request was not succeed or timed out after 3 attempts
     */
    @Throws(Exception::class)
    private fun WebApp.kuduZipDeploy(zipFile: File, processHandler: RunProcessHandler) {

        val session = WebAppDeploySession(publishingProfile.gitUsername(), publishingProfile.gitPassword())

        var success = false
        var uploadCount = 0
        var response: Response? = null

        try {
            do {
                processHandler.setText(String.format(ZIP_DEPLOY_START_PUBLISHING, uploadCount + 1, UPLOADING_MAX_TRY))

                try {
                    response = session.publishZip(
                            "https://" + name().toLowerCase() + URL_KUDU_ZIP_DEPLOY,
                            zipFile,
                            DEPLOY_TIMEOUT_MS)
                    success = response.isSuccessful
                } catch (e: Throwable) {
                    processHandler.setText(String.format(ZIP_DEPLOY_PUBLISH_FAIL, e))
                    e.printStackTrace()
                }

            } while (!success && ++uploadCount < UPLOADING_MAX_TRY && isWaitFinished())

            if (response == null || !success) {
                val message = "$ZIP_DEPLOY_PUBLISH_FAIL: Response code: ${response?.code()}. Response message: ${response?.message()}"
                processHandler.setText(message)
                throw Exception(message)
            }

            processHandler.setText(ZIP_DEPLOY_PUBLISH_SUCCESS)

        } finally {
            response?.body()?.close()
        }
    }

    /**
     * Sleep for [timeout] ms
     */
    private fun isWaitFinished(timeout: Long = SLEEP_TIME_MS): Boolean {
        try {
            Thread.sleep(timeout)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return true
    }

    /**
     * Move focus to Run tool window to switch back to the running process
     */
    private fun requestRunWindowFocus() {
        try {
            ToolWindowManager.getInstance(project).invokeLater {
                val window = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)
                if (window != null && window.isAvailable)
                    window.show(null)
            }
        } catch (e: Throwable) {
            // Ignore if we unable to switch back to Run tool window (it is not alive and we cannot log the error)
        }
    }

    private fun refreshWebAppAfterPublish(webApp: WebApp, model: AzureDotNetWebAppSettingModel) {
        model.resetOnPublish(webApp)
        AzureDotNetWebAppMvpModel.refreshSubscriptionToWebAppMap()
    }

    private fun refreshDatabaseAfterPublish(sqlDatabase: SqlDatabase, model: AzureDotNetWebAppSettingModel) {
        model.resetOnPublish(sqlDatabase)
        AzureDatabaseMvpModel.refreshSqlServerToSqlDatabaseMap()
    }

    //endregion Deploy
}
