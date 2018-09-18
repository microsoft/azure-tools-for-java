package com.microsoft.intellij.runner.webapp.webappconfig.runstate

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.io.ZipUtil
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.publishing.base.MsBuildPublishingService
import com.jetbrains.rider.util.idea.application
import com.jetbrains.rider.util.idea.toIOFile
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.RuntimeStack
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.utils.WebAppDeploySession
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants
import okhttp3.Response
import org.jetbrains.concurrency.AsyncPromise
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipOutputStream

object WebAppRunState {

    private const val URL_AZURE_BASE = ".azurewebsites.net"
    private const val URL_KUDU_BASE = ".scm$URL_AZURE_BASE"
    private const val URL_KUDU_ZIP_DEPLOY = "$URL_KUDU_BASE/api/zipdeploy"

    private const val COLLECT_ARTIFACTS_TIMEOUT_MS = 180000L
    private const val DEPLOY_TIMEOUT_MS = 180000L
    private const val SLEEP_TIME_MS = 5000L
    private const val UPLOADING_MAX_TRY = 3

    var projectAssemblyRelativePath = ""

    fun webAppStart(webApp: WebApp, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(UiConstants.WEB_APP_START, webApp.name()))
        webApp.start()
    }

    fun webAppStop(webApp: WebApp, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(UiConstants.WEB_APP_STOP, webApp.name()))
        webApp.stop()
    }

    fun getOrCreateWebAppFromConfiguration(model: AzureDotNetWebAppSettingModel.WebAppModel,
                                           processHandler: RunProcessHandler): WebApp {

        if (model.isCreatingWebApp) {
            processHandler.setText(String.format(UiConstants.WEB_APP_CREATE, model.webAppName))

            if (model.webAppName.isEmpty()) throw Exception(UiConstants.WEB_APP_NAME_NOT_DEFINED)
            if (model.resourceGroupName.isEmpty()) throw Exception(UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
            val operatingSystem = model.operatingSystem

            val webAppDefinition = AzureDotNetWebAppMvpModel.WebAppDefinition(
                    model.webAppName, model.isCreatingResourceGroup, model.resourceGroupName)

            val webApp =
                    if (model.isCreatingAppServicePlan) {
                        if (model.appServicePlanName.isEmpty()) throw Exception(UiConstants.APP_SERVICE_PLAN_NAME_NOT_DEFINED)
                        if (model.location.isEmpty()) throw Exception(UiConstants.APP_SERVICE_PLAN_LOCATION_NOT_DEFINED)
                        val pricingTier = model.pricingTier
                        val appServicePlanDefinition = AzureDotNetWebAppMvpModel.AppServicePlanDefinition(model.appServicePlanName, pricingTier, model.location)

                        if (operatingSystem == OperatingSystem.WINDOWS) {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewWindowsAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    model.netFrameworkVersion)
                        } else {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewLinuxAppServicePlan(
                                    model.subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    model.netCoreRuntime)
                        }
                    } else {
                        if (model.appServicePlanId.isEmpty()) throw Exception(UiConstants.APP_SERVICE_PLAN_ID_NOT_DEFINED)

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
                                    model.netCoreRuntime)
                        }
                    }

            processHandler.setText(String.format(UiConstants.WEB_APP_CREATE_SUCCESSFUL, webApp.id()))
            return webApp
        }

        processHandler.setText(String.format(UiConstants.WEB_APP_GET_EXISTING, model.webAppId))

        if (model.webAppId.isEmpty()) throw Exception(UiConstants.WEB_APP_ID_NOT_DEFINED)
        return AzureWebAppMvpModel.getInstance().getWebAppById(model.subscriptionId, model.webAppId)
    }

    fun deployToAzureWebApp(project: Project,
                            publishableProject: PublishableProjectModel,
                            webApp: WebApp,
                            processHandler: RunProcessHandler) {

        packAndDeploy(project, publishableProject, webApp, processHandler)
        processHandler.setText(UiConstants.DEPLOY_SUCCESSFUL)
    }

    fun getWebAppUrl(webApp: WebApp): String {
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
     * @param startupCommand a command to execute for starting a web app
     * @param runtime net core runtime
     * @param processHandler a process handler to show a process message
     */
    fun setStartupCommand(webApp: WebApp,
                          startupCommand: String,
                          runtime: RuntimeStack,
                          processHandler: RunProcessHandler) {
        processHandler.setText(String.format(UiConstants.WEB_APP_SET_STARTUP_FILE, webApp.name(), startupCommand))
        webApp.update()
                .withPublicDockerHubImage("") // Hack to access .withStartUpCommand() API
                .withStartUpCommand(startupCommand)
                .withBuiltInImage(runtime)
                .apply()

        webApp.update().withoutAppSetting(UiConstants.WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME).apply()
    }

    fun addConnectionString(subscriptionId: String,
                            webApp: WebApp,
                            database: SqlDatabase,
                            connectionStringName: String,
                            adminLogin: String,
                            adminPassword: CharArray,
                            processHandler: RunProcessHandler) {

        val sqlServer = AzureDatabaseMvpModel.getSqlServerByName(subscriptionId, database.sqlServerName(), true)

        if (sqlServer == null) {
            val message = String.format(UiConstants.SQL_SERVER_CANNOT_GET, database.sqlServerName())
            processHandler.setText(String.format(UiConstants.CONNECTION_STRING_CREATE_FAILED, message))
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

    private fun packAndDeploy(project: Project,
                              publishableProject: PublishableProjectModel,
                              webApp: WebApp,
                              processHandler: RunProcessHandler) {
        try {
            processHandler.setText(String.format(UiConstants.PROJECT_ARTIFACTS_COLLECTING, publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)
            // Note: we need to do it only for Linux Azure instances (we might add this check to speed up)
            projectAssemblyRelativePath = getAssemblyRelativePath(project, publishableProject, outDir)

            processHandler.setText(String.format(UiConstants.ZIP_FILE_CREATE_FOR_PROJECT, publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            webApp.kuduZipDeploy(zipFile, processHandler)

            if (zipFile.exists()) {
                processHandler.setText(String.format(UiConstants.ZIP_FILE_DELETING, zipFile.path))
                FileUtil.delete(zipFile)
            }
        } catch (e: Throwable) {
            processHandler.setText(String.format(UiConstants.ZIP_DEPLOY_PUBLISH_FAIL, e))
            throw e
        }
    }

    /**
     * Get a relative path for an assembly name based from a project output directory
     *
     * Note: There is no a property in [PublishableProjectModel] to get a project assembly name. Right now
     *       we hack around with RunnableProject model to get this information
     *       TODO: Rework this after we add a property to [PublishableProjectModel] and set exePath
     */
    private fun getAssemblyRelativePath(project: Project,
                                        publishableProject: PublishableProjectModel,
                                        outDir: File): String {

        val defaultPath = "${publishableProject.projectName}.dll"

        val publishableProjectPath = publishableProject.projectFilePath
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull ?: return defaultPath
        val projectOutputs = runnableProjects.find { it.projectFilePath == publishableProjectPath }?.projectOutputs?.firstOrNull() ?: return defaultPath
        val assemblyName = projectOutputs.exePath.toIOFile().name

        return outDir.walk().find { it.name == assemblyName }?.relativeTo(outDir)?.path ?: defaultPath
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
                    requestRunWindowFocus(project)
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
            throw Exception(UiConstants.PROJECT_ARTIFACTS_COLLECTING_FAILED)
        }

        return outPath.toFile().canonicalFile
    }

    @Throws(FileNotFoundException::class)
    private fun zipProjectArtifacts(fromFile: File,
                                    processHandler: RunProcessHandler,
                                    deleteOriginal: Boolean = true): File {

        if (!fromFile.exists()) throw FileNotFoundException("Original file '${fromFile.path}' not found")

        try {
            val toZip = FileUtil.createTempFile(fromFile.nameWithoutExtension, ".zip", true)
            packToZip(fromFile, toZip)
            processHandler.setText(String.format(UiConstants.ZIP_FILE_CREATE_SUCCESSFUL, toZip.path))

            if (deleteOriginal)
                FileUtil.delete(fromFile)

            return toZip
        } catch (e: Throwable) {
            processHandler.setText("${UiConstants.ZIP_FILE_NOT_CREATED}: $e")
            throw e
        }
    }

    /**
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
                processHandler.setText(String.format(UiConstants.ZIP_DEPLOY_START_PUBLISHING, uploadCount + 1, UPLOADING_MAX_TRY))

                try {
                    response = session.publishZip(
                            "https://" + name().toLowerCase() + URL_KUDU_ZIP_DEPLOY,
                            zipFile,
                            DEPLOY_TIMEOUT_MS)
                    success = response.isSuccessful
                } catch (e: Throwable) {
                    processHandler.setText(String.format(UiConstants.ZIP_DEPLOY_PUBLISH_FAIL, e))
                    e.printStackTrace()
                }

            } while (!success && ++uploadCount < UPLOADING_MAX_TRY && isWaitFinished())

            if (response == null || !success) {
                val message = "${UiConstants.ZIP_DEPLOY_PUBLISH_FAIL}: Response code: ${response?.code()}. Response message: ${response?.message()}"
                processHandler.setText(message)
                throw Exception(message)
            }

            processHandler.setText(UiConstants.ZIP_DEPLOY_PUBLISH_SUCCESS)

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
    private fun requestRunWindowFocus(project: Project) {
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

    private fun updateWithConnectionString(webApp: WebApp, name: String, value: String, processHandler: RunProcessHandler) {
        processHandler.setText(String.format(UiConstants.CONNECTION_STRING_CREATING, name))
        webApp.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()
    }
}