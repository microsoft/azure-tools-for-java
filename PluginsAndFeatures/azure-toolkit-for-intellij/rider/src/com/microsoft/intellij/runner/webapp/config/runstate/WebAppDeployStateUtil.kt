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

package com.microsoft.intellij.runner.webapp.config.runstate

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.util.idea.getLogger
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.RuntimeStack
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import org.jetbrains.plugins.azure.deploy.NotificationConstant
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
import java.util.*

object WebAppDeployStateUtil {

    private val logger = getLogger<WebAppDeployStateUtil>()

    private val activityNotifier = AzureDeploymentProgressNotification(null)

    fun webAppStart(app: WebApp, processHandler: RunProcessHandler) =
            appStart(app, processHandler, String.format(UiConstants.WEB_APP_START, app.name()), NotificationConstant.WEB_APP_START)

    fun webAppStop(app: WebApp, processHandler: RunProcessHandler) =
            appStop(app, processHandler, String.format(UiConstants.WEB_APP_STOP, app.name()), NotificationConstant.WEB_APP_STOP)

    fun getOrCreateWebAppFromConfiguration(model: WebAppPublishModel, processHandler: RunProcessHandler): WebApp {
        val subscriptionId = model.subscription?.subscriptionId() ?: throw RuntimeException(UiConstants.SUBSCRIPTION_NOT_DEFINED)

        if (model.isCreatingNewApp) {
            processHandler.setText(String.format(UiConstants.WEB_APP_CREATE, model.appName))

            if (model.appName.isEmpty()) throw RuntimeException(UiConstants.WEB_APP_NAME_NOT_DEFINED)
            if (model.resourceGroupName.isEmpty()) throw RuntimeException(UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)
            val operatingSystem = model.operatingSystem

            val webAppDefinition = AzureDotNetWebAppMvpModel.WebAppDefinition(
                    model.appName, model.isCreatingResourceGroup, model.resourceGroupName)

            val webApp =
                    if (model.isCreatingAppServicePlan) {
                        if (model.appServicePlanName.isEmpty()) throw RuntimeException(UiConstants.APP_SERVICE_PLAN_NAME_NOT_DEFINED)
                        val pricingTier = model.pricingTier
                        val appServicePlanDefinition = AzureDotNetWebAppMvpModel.AppServicePlanDefinition(model.appServicePlanName, pricingTier, model.location)

                        if (operatingSystem == OperatingSystem.WINDOWS) {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewWindowsAppServicePlan(
                                    subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    model.netFrameworkVersion)
                        } else {
                            AzureDotNetWebAppMvpModel.createWebAppWithNewLinuxAppServicePlan(
                                    subscriptionId,
                                    webAppDefinition,
                                    appServicePlanDefinition,
                                    model.netCoreRuntime)
                        }
                    } else {
                        if (model.appServicePlanId.isEmpty()) throw RuntimeException(UiConstants.APP_SERVICE_PLAN_ID_NOT_DEFINED)

                        if (operatingSystem == OperatingSystem.WINDOWS) {
                            AzureDotNetWebAppMvpModel.createWebAppWithExistingWindowsAppServicePlan(
                                    subscriptionId,
                                    webAppDefinition,
                                    model.appServicePlanId,
                                    model.netFrameworkVersion)
                        } else {
                            AzureDotNetWebAppMvpModel.createWebAppWithExistingLinuxAppServicePlan(
                                    subscriptionId,
                                    webAppDefinition,
                                    model.appServicePlanId,
                                    model.netCoreRuntime)
                        }
                    }

            val message = String.format(UiConstants.WEB_APP_CREATE_SUCCESSFUL, webApp.name())
            processHandler.setText(message)
            activityNotifier.notifyProgress(NotificationConstant.WEB_APP_CREATE, Date(), webApp.defaultHostName(), 100, message)
            return webApp
        }

        processHandler.setText(String.format(UiConstants.WEB_APP_GET_EXISTING, model.appId))
        if (model.appId.isEmpty()) throw RuntimeException(UiConstants.WEB_APP_ID_NOT_DEFINED)

        return AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, model.appId)
    }

    fun deployToAzureWebApp(project: Project,
                            publishableProject: PublishableProjectModel,
                            webApp: WebApp,
                            processHandler: RunProcessHandler) {

        packAndDeploy(project, publishableProject, webApp, processHandler)
        processHandler.setText(UiConstants.DEPLOY_SUCCESSFUL)
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
        val message = String.format(UiConstants.WEB_APP_SET_STARTUP_FILE, webApp.name(), startupCommand)
        processHandler.setText(message)
        webApp.update()
                .withPublicDockerHubImage("") // Hack to access .withStartUpCommand() API
                .withStartUpCommand(startupCommand)
                .withBuiltInImage(runtime)
                .apply()

        webApp.update().withoutAppSetting(UiConstants.WEB_APP_SETTING_DOCKER_CUSTOM_IMAGE_NAME).apply()
        activityNotifier.notifyProgress(NotificationConstant.WEB_APP_UPDATE, Date(), webApp.defaultHostName(), 100, message)
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
            val message = String.format(UiConstants.SQL_SERVER_CANNOT_GET, database.sqlServerName())
            processHandler.setText(String.format(UiConstants.CONNECTION_STRING_CREATE_FAILED, message))
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
            processHandler.setText(String.format(UiConstants.PROJECT_ARTIFACTS_COLLECTING, publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)
            // Note: we need to do it only for Linux Azure instances (we might add this check to speed up)
            projectAssemblyRelativePath = getAssemblyRelativePath(publishableProject, outDir)

            processHandler.setText(String.format(UiConstants.ZIP_FILE_CREATE_FOR_PROJECT, publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            KuduClient.kuduZipDeploy(zipFile, webApp.publishingProfile, webApp.name(), processHandler)

            if (zipFile.exists()) {
                processHandler.setText(String.format(UiConstants.ZIP_FILE_DELETING, zipFile.path))
                FileUtil.delete(zipFile)
            }
        } catch (e: Throwable) {
            logger.error(e)
            processHandler.setText("${UiConstants.ZIP_DEPLOY_PUBLISH_FAIL}: $e")
            throw RuntimeException(UiConstants.ZIP_DEPLOY_PUBLISH_FAIL, e)
        }
    }

    private fun updateWithConnectionString(webApp: WebApp, name: String, value: String, processHandler: RunProcessHandler) {
        val message = String.format(UiConstants.CONNECTION_STRING_CREATING, name)

        processHandler.setText(message)
        webApp.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()

        activityNotifier.notifyProgress(NotificationConstant.WEB_APP_UPDATE, Date(), webApp.defaultHostName(), 100, message)
    }
}