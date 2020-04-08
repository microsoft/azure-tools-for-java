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

package com.microsoft.azuretools.core.mvp.model.database

import com.microsoft.azure.management.sql.DatabaseEdition
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel.listSqlServers
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel.refreshSubscriptionToSqlServerMap
import java.io.IOException
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

object AzureSqlDatabaseMvpModel {

    private val logger = Logger.getLogger(this::class.java.name)

    private val sqlServerToSqlDatabasesMap = ConcurrentHashMap<SqlServer, List<SqlDatabase>>()

    private const val DEFAULT_COLLATION = "SQL_Latin1_General_CP1_CI_AS"

    fun refreshSqlServerToSqlDatabaseMap() {
        refreshSubscriptionToSqlServerMap()
        val sqlServers = listSqlServers()

        for (sqlServerRes in sqlServers) {
            listSqlDatabasesBySqlServer(sqlServerRes.resource, true)
        }
    }

    fun listSqlDatabasesByServerId(subscriptionId: String, sqlServerId: String): List<SqlDatabase> {
        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val sqlServer = azure.sqlServers().getById(sqlServerId)
            return sqlServer.databases().list()
        } catch (e: Throwable) {
            logger.warning(e.toString())
        }

        return ArrayList()
    }

    fun listSqlDatabasesBySubscriptionId(subscriptionId: String): List<ResourceEx<SqlDatabase>> {
        val sqlDatabaseList = ArrayList<ResourceEx<SqlDatabase>>()

        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val sqlServers = azure.sqlServers().list()

            for (sqlServer in sqlServers) {
                val sqlDatabases = sqlServer.databases().list()
                for (sqlDatabase in sqlDatabases) {
                    sqlDatabaseList.add(ResourceEx(sqlDatabase, subscriptionId))
                }
            }
        } catch (e: IOException) {
            logger.warning(e.toString())
        }

        return sqlDatabaseList
    }

    fun listSqlDatabases(): List<ResourceEx<SqlDatabase>> {
        val sqlDatabases = ArrayList<ResourceEx<SqlDatabase>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {
            sqlDatabases.addAll(listSqlDatabasesBySubscriptionId(subscription.subscriptionId()))
        }

        return sqlDatabases
    }

    fun getSqlDatabaseById(subscriptionId: String, databaseId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().databases().getById(databaseId)

    fun deleteDatabase(subscriptionId: String, databaseId: String) {
        val sqlDatabases = listSqlDatabasesBySubscriptionId(subscriptionId)
        for (sqlDatabaseRes in sqlDatabases) {
            val sqlDatabase = sqlDatabaseRes.resource
            if (sqlDatabase.id() == databaseId) {
                sqlDatabase.delete()
                return
            }
        }
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

    fun createSqlDatabase(databaseName: String,
                          sqlServer: SqlServer,
                          collation: String = DEFAULT_COLLATION,
                          edition: DatabaseEdition = DatabaseEdition.BASIC,
                          serviceObjectiveName: ServiceObjectiveName = ServiceObjectiveName.BASIC) =
            sqlServer.databases()
                    .define(databaseName)
                    .withEdition(edition)
                    .withServiceObjective(serviceObjectiveName)
                    .withCollation(collation)
                    .create()

    fun listDatabaseEditions(): List<DatabaseEdition> {
        val databaseEditions = mutableListOf<DatabaseEdition>()
        val editions = DatabaseEdition::class.java.declaredFields
        val editionsArraySize = editions.size

        for (editionIndex in 0 until editionsArraySize) {
            val field = editions[editionIndex]
            val modifier = field.modifiers
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                val pt = field.get(null as Any?) as DatabaseEdition
                databaseEditions.add(pt)
            }
        }

        return databaseEditions
    }
}
