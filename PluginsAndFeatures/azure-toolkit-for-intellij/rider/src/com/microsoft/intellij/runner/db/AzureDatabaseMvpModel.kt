/**
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.db

import com.jetbrains.rd.util.concurrentMapOf
import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory
import rx.Observable
import java.io.IOException
import java.lang.reflect.Modifier

object AzureDatabaseMvpModel {

    private val LOG = getLogger<AzureDatabaseMvpModel>()

    private val subscriptionIdToSqlServersMap = concurrentMapOf<String, List<ResourceEx<SqlServer>>>()
    private val sqlServerToSqlDatabasesMap = concurrentMapOf<SqlServer, List<SqlDatabase>>()

    fun refreshSubscriptionToSqlServerMap() {
        val azureManager = AuthMethodManager.getInstance().azureManager ?: return
        val subscriptions = azureManager.subscriptions

        for (subscription in subscriptions) {
            listSqlServersBySubscriptionId(subscription.subscriptionId(), true)
        }
    }

    fun refreshSqlServerToSqlDatabaseMap() {
        refreshSubscriptionToSqlServerMap()
        for ((_, sqlServers) in subscriptionIdToSqlServersMap) {
            for (sqlServerRes in sqlServers) {
                listSqlDatabasesBySqlServer(sqlServerRes.resource, true)
            }
        }
    }

    fun listSqlServersBySubscriptionId(subscriptionId: String, force: Boolean = false): List<ResourceEx<SqlServer>> {
        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            val sqlServers = subscriptionIdToSqlServersMap[subscriptionId]
            if (sqlServers != null) return sqlServers
        }

        try {
            val sqlServerList = listSqlServersBySubscriptionId(subscriptionId).map { ResourceEx(it, subscriptionId) }
            subscriptionIdToSqlServersMap[subscriptionId] = sqlServerList
            return sqlServerList
        } catch (e: IOException) {
            LOG.error(e)
        }

        return listOf()
    }

    fun getSqlServerById(subscriptionId: String, sqlServerId: String): SqlServer {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().getById(sqlServerId)
    }

    fun getSqlServerByName(subscriptionId: String,
                           name: String,
                           force: Boolean = false): SqlServer? {

        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            val sqlServers = subscriptionIdToSqlServersMap[subscriptionId]
            if (sqlServers != null) {
                return sqlServers.find { it.resource.name() == name }?.resource
            }
        }

        return listSqlServersBySubscriptionId(subscriptionId, true).find { it.resource.name() == name }?.resource
    }

    fun createSqlServer(subscriptionId: String,
                        sqlServerName: String,
                        region: String,
                        isCreatingResourceGroup: Boolean,
                        resourceGroupName: String,
                        sqlServerAdminLogin: String,
                        sqlServerAdminPass: CharArray): SqlServer {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)

        val serverWithRegion = azure.sqlServers().define(sqlServerName).withRegion(region)

        val serverWithResourceGroup =
                if (isCreatingResourceGroup) serverWithRegion.withNewResourceGroup(resourceGroupName)
                else serverWithRegion.withExistingResourceGroup(resourceGroupName)

        return serverWithResourceGroup
                .withAdministratorLogin(sqlServerAdminLogin)
                .withAdministratorPassword(sqlServerAdminPass.joinToString(""))
                .create()
    }

    fun getSqlServerAdminLoginAsync(subscriptionId: String, database: SqlDatabase): Observable<String> {
        return Observable.fromCallable<String> {
            val sqlServer =
                    AzureDatabaseMvpModel.listSqlServersBySubscriptionId(subscriptionId)
                            .first { it.name() == database.sqlServerName() }
            sqlServer.administratorLogin()
        }.subscribeOn(SchedulerProviderFactory.getInstance().schedulerProvider.io())
    }

    fun listSqlDatabasesBySqlServer(sqlServer: SqlServer,
                                    force: Boolean = false): List<SqlDatabase> {
        if (!force && sqlServerToSqlDatabasesMap.containsKey(sqlServer)) {
            val sqlDatabases = sqlServerToSqlDatabasesMap[sqlServer]
            if (sqlDatabases != null) return sqlDatabases
        }

        val databases = sqlServer.databases().list()
        sqlServerToSqlDatabasesMap[sqlServer] = databases

        return databases
    }

    fun listSqlDatabasesBySubscriptionId(subscriptionId: String): List<SqlDatabase> {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().list().flatMap { it.databases().list() }
    }

    fun createSqlDatabase(sqlServer: SqlServer, databaseName: String, collation: String): SqlDatabase {

        return sqlServer.databases()
                .define(databaseName)
                .withMaxSizeBytes(1073741824)
                .withEdition(DatabaseEditions.STANDARD)
                .withServiceObjective(ServiceObjectiveName.S0)
                .withCollation(collation)
                .create()
    }

    fun listDatabaseEditions(): List<DatabaseEditions> {
        val databaseEditions = mutableListOf<DatabaseEditions>()
        val editions = DatabaseEditions::class.java.declaredFields
        val editionsArraySize = editions.size

        for (editionIndex in 0 until editionsArraySize) {
            val field = editions[editionIndex]
            val modifier = field.modifiers
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                val pt = field.get(null as Any?) as DatabaseEditions
                databaseEditions.add(pt)
            }
        }

        return databaseEditions
    }

    private fun listSqlServersBySubscriptionId(subscriptionId: String): List<SqlServer> {
        return AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().list()
    }
}
