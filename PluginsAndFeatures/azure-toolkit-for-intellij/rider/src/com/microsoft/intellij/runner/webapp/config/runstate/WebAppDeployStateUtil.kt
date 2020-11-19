/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config.runstate

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.RuntimeStack
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import com.microsoft.intellij.helpers.UiConstants
import com.microsoft.intellij.helpers.deploy.KuduClient
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.appStart
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.appStop
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.collectProjectArtifacts
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.generateConnectionString
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.getAssemblyRelativePath
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.projectAssemblyRelativePath
import com.microsoft.intellij.runner.appbase.config.runstate.AppDeployStateUtil.zipProjectArtifacts
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.util.Date

object WebAppDeployStateUtil {

    private val logger = Logger.getInstance(WebAppDeployStateUtil::class.java)

    private val activityNotifier = AzureDeploymentProgressNotification()

    fun webAppStart(app: WebApp, processHandler: RunProcessHandler) =
            appStart(app = app,
                    processHandler = processHandler,
                    progressMessage = message("progress.publish.web_app.start", app.name()),
                    notificationTitle = message("tool_window.azure_activity_log.publish.web_app.start"))

    fun webAppStop(app: WebApp, processHandler: RunProcessHandler) =
            appStop(app = app,
                    processHandler = processHandler,
                    progressMessage = message("progress.publish.web_app.stop", app.name()),
                    notificationTitle = message("tool_window.azure_activity_log.publish.web_app.stop"))

    fun getOrCreateWebAppFromConfiguration(model: WebAppPublishModel, processHandler: RunProcessHandler): WebApp {
        val subscriptionId = model.subscription?.subscriptionId()
                ?: throw RuntimeException(message("process_event.publish.subscription.not_defined"))

        if (model.isCreatingNewApp) {
            processHandler.setText(message("process_event.publish.web_apps.creating", model.appName))

            val webApp = AzureDotNetWebAppMvpModel.createWebApp(
                    subscriptionId           = subscriptionId,
                    appName                  = model.appName,
                    isCreatingResourceGroup  = model.isCreatingResourceGroup,
                    resourceGroupName        = model.resourceGroupName,
                    operatingSystem          = model.operatingSystem,
                    isCreatingAppServicePlan = model.isCreatingAppServicePlan,
                    appServicePlanId         = model.appServicePlanId,
                    appServicePlanName       = model.appServicePlanName,
                    pricingTier              = model.pricingTier,
                    location                 = model.location,
                    netFrameworkVersion      = model.netFrameworkVersion,
                    netCoreRuntime           = model.netCoreRuntime
            )

            val stateMessage = message("process_event.publish.web_apps.create_success", webApp.name())
            processHandler.setText(stateMessage)
            activityNotifier.notifyProgress(
                    message("tool_window.azure_activity_log.publish.web_app.create"),
                    Date(),
                    webApp.defaultHostName(),
                    100,
                    stateMessage
            )

            return webApp
        }

        processHandler.setText(message("process_event.publish.web_apps.get_existing", model.appId))

        if (model.appId.isEmpty())
            throw RuntimeException(message("process_event.publish.web_apps.id_not_defined"))

        return AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, model.appId)
    }

    fun deployToAzureWebApp(project: Project,
                            publishableProject: PublishableProjectModel,
                            webApp: WebApp,
                            processHandler: RunProcessHandler) {

        packAndDeploy(project, publishableProject, webApp, processHandler)
        processHandler.setText(message("process_event.publish.deploy_succeeded"))
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
        val stateMessage = message("process_event.publish.web_apps.set_startup_file", webApp.name(), startupCommand)
        processHandler.setText(stateMessage)
        webApp.update()
                .withPublicDockerHubImage("") // Hack to access .withStartUpCommand() API
                .withStartUpCommand(startupCommand)
                .withBuiltInImage(runtime)
                .apply()

        webApp.update().withoutAppSetting(UiConstants.WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME).apply()
        activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.web_app.update"), Date(), webApp.defaultHostName(), 100, stateMessage)
    }

    fun addConnectionString(subscriptionId: String,
                            webApp: WebApp,
                            database: SqlDatabase,
                            connectionStringName: String,
                            adminLogin: String,
                            adminPassword: CharArray,
                            processHandler: RunProcessHandler) {

        val sqlServer = AzureSqlServerMvpModel.getSqlServerByName(subscriptionId, database.sqlServerName(), true)

        if (sqlServer == null) {
            val errorMessage = message("process_event.publish.sql_server.find_existing_error", database.sqlServerName())
            processHandler.setText(
                    message("process_event.publish.connection_string.create_failed", errorMessage))
            return
        }

        val fullyQualifiedDomainName = sqlServer.fullyQualifiedDomainName()
        val connectionStringValue = generateConnectionString(fullyQualifiedDomainName, database, adminLogin, adminPassword)

        updateWithConnectionString(webApp, connectionStringName, connectionStringValue, processHandler)
    }

    private fun packAndDeploy(project: Project,
                              publishableProject: PublishableProjectModel,
                              webApp: WebApp,
                              processHandler: RunProcessHandler) {
        try {
            processHandler.setText(message("process_event.publish.project.artifacts.collecting", publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)
            // Note: we need to do it only for Linux Azure instances (we might add this check to speed up)
            projectAssemblyRelativePath = getAssemblyRelativePath(publishableProject, outDir)

            processHandler.setText(message("process_event.publish.zip_deploy.file_creating", publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            webAppStop(webApp, processHandler)

            KuduClient.kuduZipDeploy(zipFile, webApp, processHandler)

            if (zipFile.exists()) {
                processHandler.setText(message("process_event.publish.zip_deploy.file_deleting", zipFile.path))
                FileUtil.delete(zipFile)
            }
        } catch (e: Throwable) {
            val errorMessage = "${message("process_event.publish.zip_deploy.fail")}: $e"
            logger.error(errorMessage)
            processHandler.setText(errorMessage)
            throw RuntimeException(message("process_event.publish.zip_deploy.fail"), e)
        }
    }

    private fun updateWithConnectionString(webApp: WebApp, name: String, value: String, processHandler: RunProcessHandler) {
        val processMessage = message("process_event.publish.connection_string.creating", name)

        processHandler.setText(processMessage)
        webApp.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()

        activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.web_app.update"), Date(), webApp.defaultHostName(), 100, processMessage)
    }
}