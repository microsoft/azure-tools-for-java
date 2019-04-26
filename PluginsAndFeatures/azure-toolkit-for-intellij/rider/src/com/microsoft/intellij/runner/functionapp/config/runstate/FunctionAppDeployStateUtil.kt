/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.functionapp.config.runstate

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.util.idea.getLogger
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
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
import com.microsoft.intellij.runner.functionapp.model.FunctionAppPublishModel
import java.util.*

object FunctionAppDeployStateUtil {

    private val logger = getLogger<FunctionAppDeployStateUtil>()

    private val activityNotifier = AzureDeploymentProgressNotification(null)

    fun functionAppStart(app: FunctionApp, processHandler: RunProcessHandler) =
            appStart(app, processHandler, String.format(UiConstants.FUNCTION_APP_START, app.name()), NotificationConstant.FUNCTION_APP_START)

    fun functionAppStop(app: FunctionApp, processHandler: RunProcessHandler) =
            appStop(app, processHandler, String.format(UiConstants.FUNCTION_APP_STOP, app.name()), NotificationConstant.FUNCTION_APP_STOP)

    fun getOrCreateFunctionAppFromConfiguration(model: FunctionAppPublishModel,
                                                processHandler: RunProcessHandler): FunctionApp {

        val subscriptionId = model.subscription?.subscriptionId()
                ?: throw RuntimeException(UiConstants.SUBSCRIPTION_NOT_DEFINED)

        if (!model.isCreatingNewApp) {
            logger.info("Use existing Function App with id: '${model.appId}'")
            processHandler.setText(String.format(UiConstants.FUNCTION_APP_GET_EXISTING, model.appId))
            return AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, model.appId)
        }

        val functionModelLog = StringBuilder("Create a new Function App with name '${model.appName}', ")
                .append("isCreateResourceGroup: ")   .append(model.isCreatingResourceGroup) .append(", ")
                .append("resourceGroupName: ")       .append(model.resourceGroupName)       .append(", ")
                .append("isCreatingAppServicePlan: ").append(model.isCreatingAppServicePlan).append(", ")
                .append("appServicePlanId: ")        .append(model.appServicePlanId)        .append(", ")
                .append("appServicePlanName: ")      .append(model.appServicePlanName)      .append(", ")
                .append("location: ")                .append(model.location)                .append(", ")
                .append("pricingTier: ")             .append(model.pricingTier)             .append(", ")
                .append("isCreatingStorageAccount: ").append(model.isCreatingStorageAccount).append(", ")
                .append("storageAccountId: ")        .append(model.storageAccountId)        .append(", ")
                .append("storageAccountName: ")      .append(model.storageAccountName)      .append(", ")
                .append("storageAccountType: ")      .append(model.storageAccountType)

        logger.info(functionModelLog.toString())
        processHandler.setText(String.format(UiConstants.FUNCTION_APP_CREATE, model.appName))

        if (model.appName.isEmpty()) throw RuntimeException(UiConstants.FUNCTION_APP_NAME_NOT_DEFINED)
        if (model.resourceGroupName.isEmpty()) throw RuntimeException(UiConstants.RESOURCE_GROUP_NAME_NOT_DEFINED)

        val app = AzureFunctionAppMvpModel.createFunctionApp(
                subscriptionId         = subscriptionId,
                appName                = model.appName,
                isCreateResourceGroup  = model.isCreatingResourceGroup,
                resourceGroupName      = model.resourceGroupName,
                isCreateAppServicePlan = model.isCreatingAppServicePlan,
                appServicePlanId       = model.appServicePlanId,
                appServicePlanName     = model.appServicePlanName,
                region                 = model.location,
                pricingTier            = model.pricingTier,
                isCreateStorageAccount = model.isCreatingStorageAccount,
                storageAccountId       = model.storageAccountId,
                storageAccountName     = model.storageAccountName,
                storageAccountType     = model.storageAccountType)

        val message = String.format(UiConstants.FUNCTION_APP_CREATE_SUCCESSFUL, app.name())
        processHandler.setText(message)
        activityNotifier.notifyProgress(NotificationConstant.FUNCTION_APP_CREATE, Date(), app.defaultHostName(), 100, message)
        return app
    }

    fun addConnectionString(subscriptionId: String,
                            app: FunctionApp,
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

        updateWithConnectionString(app, connectionStringName, connectionStringValue, processHandler)
    }

    fun deployToAzureFunctionApp(project: Project,
                                 publishableProject: PublishableProjectModel,
                                 app: FunctionApp,
                                 processHandler: RunProcessHandler) {

        packAndDeploy(project, publishableProject, app, processHandler)
        processHandler.setText(UiConstants.DEPLOY_SUCCESSFUL)
    }

    private fun packAndDeploy(project: Project,
                              publishableProject: PublishableProjectModel,
                              app: FunctionApp,
                              processHandler: RunProcessHandler) {
        try {
            processHandler.setText(String.format(UiConstants.PROJECT_ARTIFACTS_COLLECTING, publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)
            // Note: we need to do it only for Linux Azure instances (we might add this check to speed up)
            projectAssemblyRelativePath = getAssemblyRelativePath(publishableProject, outDir)

            processHandler.setText(String.format(UiConstants.ZIP_FILE_CREATE_FOR_PROJECT, publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            functionAppStop(app, processHandler)

            KuduClient.kuduZipDeploy(zipFile, app.publishingProfile, app.name(), processHandler)

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

    private fun updateWithConnectionString(app: FunctionApp, name: String, value: String, processHandler: RunProcessHandler) {
        val message = String.format(UiConstants.CONNECTION_STRING_CREATING, name)

        processHandler.setText(message)
        app.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()

        activityNotifier.notifyProgress(NotificationConstant.FUNCTION_APP_UPDATE, Date(), app.defaultHostName(), 100, message)
    }
}
