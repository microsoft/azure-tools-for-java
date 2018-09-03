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
import com.jetbrains.rider.util.concurrent.SyncEvent
import com.jetbrains.rider.util.idea.application
import com.microsoft.azure.management.appservice.PublishingProfile
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel
import com.microsoft.azuretools.utils.AzureUIRefreshCore
import com.microsoft.azuretools.utils.AzureUIRefreshEvent
import com.microsoft.intellij.runner.AzureRunProfileState
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.util.WebAppDeploySession
import okhttp3.Response
import java.io.*
import java.util.zip.ZipOutputStream

class RiderWebAppRunState(project: Project,
                          private val myModel: AzureDotNetWebAppSettingModel) : AzureRunProfileState<WebApp>(project) {

    companion object {
        private const val TARGET_NAME = "WebApp"

        // Subscription
        private const val SUBSCRIPTION_ID_NOT_DEFIED = "Subscription ID is not defined"

        private const val PROJECT_NOT_DEFINED = "Project is not defined"

        private const val WEB_APP_START = "Start Web App '%s'..."
        private const val WEB_APP_STOP = "Stop Web App '%s'..."
        private const val WEB_APP_CREATE = "Creating Web App '%s'..."
        private const val WEB_APP_CREATE_SUCCESSFUL = "Web App  is created successfully, id '%s'"
        private const val WEB_APP_GET_EXISTING = "Get existing Web App with Id '%s'"
        private const val WEB_APP_ID_NOT_DEFINED = "Web App ID is not defined"
        private const val WEB_APP_NAME_NOT_DEFINED = "Web App Name is not defined"

        private const val RESOURCE_GROUP_NAME_NOT_DEFINED = "Resource Group Name is not defined"

        private const val APP_SERVICE_PLAN_ID_NOT_DEFINED = "App Service Plan ID is not defined"
        private const val APP_SERVICE_PLAN_NAME_NOT_DEFINED = "App Service Plan Name is not defined"
        private const val APP_SERVICE_PLAN_REGION_NOT_DEFINED = "App Service Plan Region is not defined"
        private const val APP_SERVICE_PLAN_PRICING_TIER_NOT_DEFINED = "App Service Plan Pricing Tier is not defined"

        private const val DEPLOY_SUCCESSFUL = "Deploy successfully!"
        private const val DEPLOY_FAILED = "Deploy failed"
        private const val DEPLOY_GET_CREDENTIAL = "Getting deployment credential..."

        private const val PROJECT_ARTIFACTS_COLLECTING = "Collecting '%s' project artifacts..."
        private const val PROJECT_ARTIFACTS_COLLECTING_FAILED = "Failed collecting project artifacts. Please see Build output"

        private const val ZIP_FILE_CREATE_FOR_PROJECT = "Creating '%s' project ZIP..."
        private const val ZIP_FILE_CREATE_SUCCESSFUL = "Project ZIP is created: '%s'"
        private const val ZIP_FILE_NOT_CREATED = "Unable to create a ZIP file"
        private const val ZIP_FILE_DELETING = "Deleting ZIP file '%s'"
        private const val ZIP_DEPLOY_START_PUBLISHING = "Publishing ZIP file..."
        private const val ZIP_DEPLOY_PUBLISH_SUCCESS = "Published ZIP file successfully"
        private const val ZIP_DEPLOY_PUBLISH_FAIL = "Fail publishing ZIP file"

        private const val URL_AZURE_BASE = ".azurewebsites.net"
        private const val URL_KUDU_BASE = ".scm$URL_AZURE_BASE"
        private const val URL_KUDU_ZIP_DEPLOY = "$URL_KUDU_BASE/api/zipdeploy"

        private const val COLLECT_ARTIFACTS_TIMEOUT = 180000L
        private const val DEPLOY_TIMEOUT = 180000L
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
                                     telemetryMap: MutableMap<String, String>): WebApp? {

        val publishableProject = myModel.publishableProject
                ?: throw Exception(String.format(PROJECT_NOT_DEFINED))

        val webApp = getOrCreateWebAppFromConfiguration(myModel, processHandler)

        webAppStop(webApp, processHandler)
        deployToAzureWebApp(publishableProject, webApp, processHandler)
        webAppStart(webApp, processHandler)

        val url = getWebAppUrl(webApp)
        processHandler.setText(DEPLOY_SUCCESSFUL)
        processHandler.setText("URL: $url")

        return webApp
    }

    override fun onSuccess(result: WebApp, processHandler: RunProcessHandler) {
        processHandler.notifyComplete()
        if (myModel.isCreatingWebApp && AzureUIRefreshCore.listeners != null) {
            AzureUIRefreshCore.execute(AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null))
        }
        updateConfigurationDataModel(result)
        AzureWebAppMvpModel.getInstance().listWebApps(true)
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

        val subscriptionId = model.subscriptionId ?: throw Exception(SUBSCRIPTION_ID_NOT_DEFIED)

        if (model.isCreatingWebApp) {
            processHandler.setText(String.format(WEB_APP_CREATE, myModel.webAppName))

            val webAppName = model.webAppName ?: throw Exception(WEB_APP_NAME_NOT_DEFINED)
            val resourceGroupName = model.resourceGroupName ?: throw Exception(RESOURCE_GROUP_NAME_NOT_DEFINED)

            val webApp =
                    if (model.isCreatingAppServicePlan) {
                        val appServicePlanName = model.appServicePlanName ?: throw Exception(APP_SERVICE_PLAN_NAME_NOT_DEFINED)
                        val pricingTier = model.pricingTier ?: throw Exception(APP_SERVICE_PLAN_PRICING_TIER_NOT_DEFINED)
                        val region = model.region ?: throw Exception(APP_SERVICE_PLAN_REGION_NOT_DEFINED)
                        AzureDotNetWebAppMvpModel.createWebAppWithNewAppServicePlan(
                                subscriptionId,
                                webAppName,
                                appServicePlanName,
                                pricingTier,
                                region,
                                model.isCreatingResourceGroup,
                                resourceGroupName)
                    } else {
                        val appServicePlanId = model.appServicePlanId ?: throw Exception(APP_SERVICE_PLAN_ID_NOT_DEFINED)
                        AzureDotNetWebAppMvpModel.createWebAppWithExistingAppServicePlan(
                                subscriptionId,
                                webAppName,
                                appServicePlanId,
                                model.isCreatingResourceGroup,
                                resourceGroupName)
                    }

            processHandler.setText(String.format(WEB_APP_CREATE_SUCCESSFUL, webApp.id()))
            return webApp
        }

        processHandler.setText(String.format(WEB_APP_GET_EXISTING, model.webAppId))

        val webAppId = model.webAppId ?: throw Exception(WEB_APP_ID_NOT_DEFINED)
        return AzureDotNetWebAppMvpModel.getDotNetWebApp(subscriptionId, webAppId)
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
     * Reset [WebAppSettingModel] after web app deployment is succeed
     *
     * @param webApp - [WebApp] instance that was deployed
     */
    private fun updateConfigurationDataModel(webApp: WebApp) {
        myModel.isCreatingWebApp = false
        myModel.webAppId = webApp.id()
        myModel.webAppName = null

        myModel.isCreatingResourceGroup = false
        myModel.resourceGroupName = webApp.resourceGroupName()

        myModel.isCreatingAppServicePlan = false
        myModel.appServicePlanId = webApp.appServicePlanId()
        myModel.appServicePlanName = null
    }

    //endregion Web App

    //region Deploy

    /**
     * Deploy an artifacts to a created web app
     *
     * @param publishableProject - a web app project to be published to Azure
     * @param webApp - Web App instance to use for deployment
     * @param processHandler - a process handler to show a process message
     */
    private fun deployToAzureWebApp(publishableProject: PublishableProjectModel,
                                    webApp: WebApp,
                                    processHandler: RunProcessHandler) {

        processHandler.setText(DEPLOY_GET_CREDENTIAL)
        zipDeploy(publishableProject, webApp.name(), webApp.publishingProfile, processHandler)
    }

    /**
     * Deploy to Web App using KUDU Zip-Deploy
     *
     * @param webAppName - web app to deploy at
     * @param profile - publish profile with credentials
     * @param processHandler - a process handler to show a process message
     */
    private fun zipDeploy(publishableProject: PublishableProjectModel,
                          webAppName: String,
                          profile: PublishingProfile,
                          processHandler: RunProcessHandler) {

        var zipFile: File? = null
        try {
            processHandler.setText(String.format(PROJECT_ARTIFACTS_COLLECTING, myModel.publishableProject?.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject, processHandler)

            processHandler.setText(String.format(ZIP_FILE_CREATE_FOR_PROJECT, myModel.publishableProject?.projectName))
            zipFile = zipProjectArtifacts(outDir, processHandler)

            processHandler.setText(ZIP_DEPLOY_START_PUBLISHING)
            publishZipToAzure(webAppName, zipFile, profile, processHandler)
        } catch (e: Throwable) {
            processHandler.setText("$DEPLOY_FAILED: $e")
            throw e
        } finally {
            if (zipFile != null && zipFile.exists()) {
                processHandler.setText(String.format(ZIP_FILE_DELETING, zipFile.path))
                FileUtil.delete(zipFile)
            }
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
                                        publishableProject: PublishableProjectModel,
                                        processHandler: RunProcessHandler): File {

        // Get out parameters
        val publishService = MsBuildPublishingService.getInstance(project)
        val (targetProperties, outPath) = publishService.getPublishToTempDirParameterAndPath()

        val event = SyncEvent()
        application.invokeLater {

            val onFinish: (result: BuildResultKind) -> Unit = {
                if (it != BuildResultKind.Successful) processHandler.setText(PROJECT_ARTIFACTS_COLLECTING_FAILED)
                requestRunWindowFocus()
                event.set()
            }

            if (publishableProject.isDotNetCore) {
                publishService.invokeMsBuild(publishableProject.projectFilePath, listOf(targetProperties), false, false, onFinish)
            } else {
                publishService.webPublishToFileSystem(publishableProject.projectFilePath, outPath, false, false, onFinish)
            }
        }
        event.wait(COLLECT_ARTIFACTS_TIMEOUT)

        return outPath.toFile()
    }

    /**
     * Archive a directory
     *
     * @param fromFile - file to be added to ZIP archive
     * @param processHandler - a process handler to show a process message
     * @param deleteOriginal - delete original file that is used for compression
     *
     * @return [File] Zip archive file
     */
    @Throws(IOException::class)
    private fun zipProjectArtifacts(fromFile: File,
                                    processHandler: RunProcessHandler,
                                    deleteOriginal: Boolean = true): File {
        try {
            val toZip = FileUtil.createTempFile(fromFile.nameWithoutExtension, ".zip", true)
            packToZip(fromFile, toZip)

            processHandler.setText(String.format(ZIP_FILE_CREATE_SUCCESSFUL, toZip.path))
            return toZip
        } catch (e: IOException) {
            processHandler.setText("$ZIP_FILE_NOT_CREATED: $e")
            throw e
        } finally {
            if (fromFile.exists() && deleteOriginal)
                FileUtil.delete(fromFile)
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
     * @param webAppName - Azure web app name
     * @param zipFile - zip file instance to be published
     * @param profile - publish profile with FTP and Git credentials
     * @param processHandler - a process handler to show a process message
     *
     * @throws [Exception] in case REST request was not succeed or timed out after 3 attempts
     */
    @Throws(Exception::class)
    private fun publishZipToAzure(webAppName: String,
                                  zipFile: File,
                                  profile: PublishingProfile,
                                  processHandler: RunProcessHandler) {

        val session = WebAppDeploySession
        session.setCredentials(profile.gitUsername(), profile.gitPassword())

        var success = false
        var count = 0
        var response: Response? = null

        try {
            do {
                try {
                    response = session.publishZip(
                            "https://" + webAppName.toLowerCase() + URL_KUDU_ZIP_DEPLOY,
                            zipFile,
                            DEPLOY_TIMEOUT)
                    success = response.isSuccessful
                } catch (e: Throwable) {
                    processHandler.setText("Attempt ${count + 1} of $UPLOADING_MAX_TRY. $ZIP_DEPLOY_PUBLISH_FAIL: $e")
                    e.printStackTrace()
                }

            } while (!success && ++count < UPLOADING_MAX_TRY && isWaitFinished())

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
        } catch (e: Exception) {
            // Intentional catch all
        }
    }

    //endregion Deploy
}
