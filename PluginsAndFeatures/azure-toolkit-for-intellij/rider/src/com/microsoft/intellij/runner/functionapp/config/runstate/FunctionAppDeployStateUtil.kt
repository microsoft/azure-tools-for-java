/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.ConnectionStringType
import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.core.mvp.model.functionapp.AzureFunctionAppMvpModel
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
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
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.util.*

object FunctionAppDeployStateUtil {

    private val logger = Logger.getInstance(FunctionAppDeployStateUtil::class.java)

    private val activityNotifier = AzureDeploymentProgressNotification()

    fun functionAppStart(app: FunctionApp, processHandler: RunProcessHandler) =
            appStart(app = app,
                    processHandler = processHandler,
                    progressMessage = message("progress.publish.function_app.start", app.name()),
                    notificationTitle = message("tool_window.azure_activity_log.publish.function_app.start"))

    fun functionAppStop(app: FunctionApp, processHandler: RunProcessHandler) =
            appStop(app = app,
                    processHandler = processHandler,
                    progressMessage = message("progress.publish.function_app.stop", app.name()),
                    notificationTitle = message("tool_window.azure_activity_log.publish.function_app.stop"))

    fun getOrCreateFunctionAppFromConfiguration(model: FunctionAppPublishModel,
                                                processHandler: RunProcessHandler): FunctionApp {

        val subscriptionId = model.subscription?.subscriptionId()
                ?: throw RuntimeException(message("process_event.publish.subscription.not_defined"))

        if (!model.isCreatingNewApp) {
            logger.info("Use existing Function App with id: '${model.appId}'")
            processHandler.setText(message("process_event.publish.function_apps.get_existing", model.appId))
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
        processHandler.setText(message("process_event.publish.function_apps.creating", model.appName))

        if (model.appName.isEmpty())
            throw RuntimeException(message("process_event.publish.function_apps.name_not_defined"))

        if (model.resourceGroupName.isEmpty())
            throw RuntimeException(message("process_event.publish.resource_group.not_defined"))

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

        val stateMessage = message("process_event.publish.function_apps.create_success", app.name())
        processHandler.setText(stateMessage)
        activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.function_app.create"), Date(), app.defaultHostName(), 100, stateMessage)
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
            val errorMessage = message("process_event.publish.sql_server.find_existing_error", database.sqlServerName())
            processHandler.setText(
                    message("process_event.publish.connection_string.create_failed", errorMessage))
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
        processHandler.setText(message("process_event.publish.deploy_succeeded"))
    }

    private fun packAndDeploy(project: Project,
                              publishableProject: PublishableProjectModel,
                              app: FunctionApp,
                              processHandler: RunProcessHandler) {
        try {
            processHandler.setText(message("process_event.publish.project.artifacts.collecting", publishableProject.projectName))
            val outDir = collectProjectArtifacts(project, publishableProject)
            // Note: we need to do it only for Linux Azure instances (we might add this check to speed up)
            projectAssemblyRelativePath = getAssemblyRelativePath(publishableProject, outDir)

            processHandler.setText(message("process_event.publish.zip_deploy.file_creating", publishableProject.projectName))
            val zipFile = zipProjectArtifacts(outDir, processHandler)

            functionAppStop(app, processHandler)

            KuduClient.kuduZipDeploy(zipFile, app, processHandler)

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

    private fun updateWithConnectionString(app: FunctionApp, name: String, value: String, processHandler: RunProcessHandler) {
        val processMessage = message("process_event.publish.connection_string.creating", name)

        processHandler.setText(processMessage)
        app.update().withConnectionString(name, value, ConnectionStringType.SQLAZURE).apply()

        activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.function_app.update"), Date(), app.defaultHostName(), 100, processMessage)
    }
}
