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

package com.microsoft.intellij.runner.database.config.deploy

import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.intellij.deploy.AzureDeploymentProgressNotification
import org.jetbrains.plugins.azure.deploy.NotificationConstant
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import com.microsoft.intellij.helpers.UiConstants
import java.util.*

object DatabaseDeployUtil {

    private val activityNotifier = AzureDeploymentProgressNotification(null)

    fun getOrCreateSqlDatabaseFromConfig(model: DatabasePublishModel, processHandler: RunProcessHandler): SqlDatabase {
        if (model.isCreatingSqlDatabase) {
            val sqlServer = getOrCreateSqlServerFromConfiguration(model, processHandler)
            return createDatabase(sqlServer, model, processHandler)
        }

        processHandler.setText(String.format(UiConstants.SQL_DATABASE_GET_EXISTING, model.databaseId))
        val sqlServerId = model.databaseId.split("/").dropLast(2).joinToString("/")
        val sqlServer = AzureSqlServerMvpModel.getSqlServerById(model.subscription?.subscriptionId() ?: "", sqlServerId)

        val databaseId = model.databaseId.split("/").last()
        val database = sqlServer.databases().get(databaseId)
        return database
    }

    private fun createDatabase(sqlServer: SqlServer,
                               model: DatabasePublishModel,
                               processHandler: RunProcessHandler): SqlDatabase {

        processHandler.setText(String.format(UiConstants.SQL_DATABASE_CREATE, model.databaseName))

        if (model.databaseName.isEmpty()) throw RuntimeException(UiConstants.SQL_DATABASE_NAME_NOT_DEFINED)
        val database = AzureSqlDatabaseMvpModel.createSqlDatabase(
                databaseName = model.databaseName,
                sqlServer = sqlServer,
                collation = model.collation)

        val message = String.format(UiConstants.SQL_DATABASE_CREATE_SUCCESSFUL, database.id())
        processHandler.setText(message)
        activityNotifier.notifyProgress(NotificationConstant.SQL_DATABASE_CREATE, Date(), null, 100, message)

        return database
    }

    private fun getOrCreateSqlServerFromConfiguration(model: DatabasePublishModel,
                                                      processHandler: RunProcessHandler): SqlServer {

        val subscriptionId = model.subscription?.subscriptionId() ?: throw RuntimeException(UiConstants.SUBSCRIPTION_NOT_DEFINED)

        if (model.isCreatingSqlServer) {
            processHandler.setText(String.format(UiConstants.SQL_SERVER_CREATE, model.sqlServerName))

            if (model.sqlServerName.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_NAME_NOT_DEFINED)
            if (model.resourceGroupName.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_RESOURCE_GROUP_NAME_NOT_DEFINED)
            if (model.sqlServerAdminLogin.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_ADMIN_LOGIN_NOT_DEFINED)
            if (model.sqlServerAdminPassword.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_ADMIN_PASSWORD_NOT_DEFINED)

            val sqlServer = AzureSqlServerMvpModel.createSqlServer(
                    subscriptionId,
                    model.sqlServerName,
                    model.location,
                    model.isCreatingResourceGroup,
                    model.resourceGroupName,
                    model.sqlServerAdminLogin,
                    model.sqlServerAdminPassword)

            val message = String.format(UiConstants.SQL_SERVER_CREATE_SUCCESSFUL, sqlServer.id())
            processHandler.setText(message)
            activityNotifier.notifyProgress(NotificationConstant.SQL_SERVER_CREATE, Date(), null, 100, message)

            return sqlServer
        }

        processHandler.setText(String.format(UiConstants.SQL_SERVER_GET_EXISTING, model.sqlServerId))

        if (model.sqlServerId.isEmpty()) throw RuntimeException(UiConstants.SQL_SERVER_ID_NOT_DEFINED)
        return AzureSqlServerMvpModel.getSqlServerById(subscriptionId, model.sqlServerId)
    }
}