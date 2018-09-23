package com.microsoft.intellij.runner.db

import com.jetbrains.rider.util.concurrentMapOf
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

    private val subscriptionIdToSqlServersMap = concurrentMapOf<String, List<ResourceEx<SqlServer>>>()
    private val sqlServerToSqlDatabasesMap = concurrentMapOf<SqlServer, List<SqlDatabase>>()

    fun refreshSubscriptionToSqlServerMap() {
        val azureManager = AuthMethodManager.getInstance().azureManager ?: return
        val subscriptions = azureManager.subscriptions

        subscriptions.forEach {
            listSqlServersBySubscriptionId(it.subscriptionId(), true)
        }
    }

    fun refreshSqlServerToSqlDatabaseMap() {
        refreshSubscriptionToSqlServerMap()
        subscriptionIdToSqlServersMap.forEach {
            val sqlServers = it.value
            sqlServers.forEach {
                listSqlDatabasesBySqlServer(it.resource, true)
            }
        }
    }

    fun listSqlServersBySubscriptionId(subscriptionId: String, force: Boolean = false): List<ResourceEx<SqlServer>> {
        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            return subscriptionIdToSqlServersMap.getValue(subscriptionId)
        }

        val sqlServerList = mutableListOf<ResourceEx<SqlServer>>()

        try {
            val sqlServersIterator = listSqlServersBySubscriptionId(subscriptionId).iterator()

            while (sqlServersIterator.hasNext()) {
                val sqlServer = sqlServersIterator.next()
                sqlServerList.add(ResourceEx(sqlServer, subscriptionId))
            }

            subscriptionIdToSqlServersMap[subscriptionId] = sqlServerList
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return sqlServerList
    }

    fun getSqlServerById(subscriptionId: String, sqlServerId: String): SqlServer {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().getById(sqlServerId)
    }

    fun getSqlServerByName(subscriptionId: String,
                           name: String,
                           force: Boolean = false): SqlServer? {

        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            return subscriptionIdToSqlServersMap.getValue(subscriptionId)
                    .find { it.resource.name() == name }?.resource
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
            return sqlServerToSqlDatabasesMap.getValue(sqlServer)
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
