/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.database.model.DatabasePublishModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import java.util.*

object DatabaseDeployUtil {

    private val activityNotifier = AzureDeploymentProgressNotification(null)

    fun getOrCreateSqlDatabaseFromConfig(model: DatabasePublishModel, processHandler: RunProcessHandler): SqlDatabase {
        if (model.isCreatingSqlDatabase) {
            val sqlServer = getOrCreateSqlServerFromConfiguration(model, processHandler)
            return createDatabase(sqlServer, model, processHandler)
        }

        processHandler.setText(message("process_event.publish.sql_db.get_existing", model.databaseId))
        val sqlServerId = model.databaseId.split("/").dropLast(2).joinToString("/")
        val sqlServer = AzureSqlServerMvpModel.getSqlServerById(model.subscription?.subscriptionId() ?: "", sqlServerId)

        val databaseId = model.databaseId.split("/").last()
        val database = sqlServer.databases().get(databaseId)
        return database
    }

    private fun createDatabase(sqlServer: SqlServer,
                               model: DatabasePublishModel,
                               processHandler: RunProcessHandler): SqlDatabase {

        processHandler.setText(message("process_event.publish.sql_db.creating", model.databaseName))

        if (model.databaseName.isEmpty())
            throw RuntimeException(message("process_event.publish.sql_db.name_not_defined"))

        val database = AzureSqlDatabaseMvpModel.createSqlDatabase(
                databaseName = model.databaseName,
                sqlServer = sqlServer,
                collation = model.collation)

        val eventMessage = message("process_event.publish.sql_db.create_success", database.id())
        processHandler.setText(eventMessage)
        activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.sql_db.create"), Date(), null, 100, eventMessage)

        return database
    }

    private fun getOrCreateSqlServerFromConfiguration(model: DatabasePublishModel,
                                                      processHandler: RunProcessHandler): SqlServer {

        val subscriptionId = model.subscription?.subscriptionId()
                ?: throw RuntimeException(message("process_event.publish.subscription.not_defined"))

        if (model.isCreatingSqlServer) {
            processHandler.setText(message("process_event.publish.sql_server.creating", model.sqlServerName))

            if (model.sqlServerName.isEmpty()) throw RuntimeException(message("process_event.publish.sql_server.name_not_defined"))
            if (model.resourceGroupName.isEmpty()) throw RuntimeException(message("process_event.publish.sql_server.resource_group_not_defined"))
            if (model.sqlServerAdminLogin.isEmpty()) throw RuntimeException(message("process_event.publish.sql_server.admin_login_not_defined"))
            if (model.sqlServerAdminPassword.isEmpty()) throw RuntimeException(message("process_event.publish.sql_server.admin_password_not_defined"))

            val sqlServer = AzureSqlServerMvpModel.createSqlServer(
                    subscriptionId,
                    model.sqlServerName,
                    model.location,
                    model.isCreatingResourceGroup,
                    model.resourceGroupName,
                    model.sqlServerAdminLogin,
                    model.sqlServerAdminPassword)

            val eventMessage = message("process_event.publish.sql_server.create_success", sqlServer.id())
            processHandler.setText(eventMessage)
            activityNotifier.notifyProgress(message("tool_window.azure_activity_log.publish.sql_server.create"), Date(), null, 100, eventMessage)

            return sqlServer
        }

        processHandler.setText(message("process_event.publish.sql_server.get_existing", model.sqlServerId))

        if (model.sqlServerId.isEmpty())
            throw RuntimeException(message("process_event.publish.sql_server.id_not_defined"))

        return AzureSqlServerMvpModel.getSqlServerById(subscriptionId, model.sqlServerId)
    }
}