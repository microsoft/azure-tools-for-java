package com.microsoft.intellij.runner.db

import com.jetbrains.rider.util.concurrentMapOf
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.ServiceObjectiveName
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.ui.base.SchedulerProviderFactory
import rx.Observable
import java.io.IOException
import java.lang.reflect.Modifier

object AzureDatabaseMvpModel {

    private const val CANNOT_GET_WEB_APP_WITH_ID = "Cannot get SQL Database with ID: "

    private val subscriptionIdToSqlServersMap = concurrentMapOf<String, List<ResourceEx<SqlServer>>>()
    private val subscriptionIdToSqlDatabasesMap = concurrentMapOf<String, List<ResourceEx<SqlDatabase>>>()

    fun cleanSqlDatabases() = subscriptionIdToSqlDatabasesMap.clear()

    fun cleanSqlServers() = subscriptionIdToSqlServersMap.clear()

    // -------------------[GET]--------------------

    fun getSqlServersBySubscriptionId(subscriptionId: String): List<SqlServer> {
        return AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().list()
    }

    // TODO: Probably, we should not use this method - use the one above instead. We should list all servers independent from resource group.
    fun getSqlServersBySubscriptionIdAndResourceGroup(subscriptionId: String, resourceGroupName: String): List<SqlServer> {
        return AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().listByResourceGroup(resourceGroupName)
    }

    //region List

    fun listSqlServersBySubscriptionId(subscriptionId: String, force: Boolean): List<ResourceEx<SqlServer>> {
        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            return subscriptionIdToSqlServersMap.getValue(subscriptionId)
        }

        val sqlServerList = mutableListOf<ResourceEx<SqlServer>>()

        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val sqlServersIterator = azure.sqlServers().list().iterator()

            while (sqlServersIterator.hasNext()) {
                val sqlServer = sqlServersIterator.next() as SqlServer
                sqlServerList.add(ResourceEx(sqlServer, subscriptionId))
            }

            subscriptionIdToSqlServersMap[subscriptionId] = sqlServerList
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return sqlServerList
    }

    fun listSqlDatabases(force: Boolean): List<ResourceEx<SqlDatabase>> {
        val sqlDatabases = mutableListOf<ResourceEx<SqlDatabase>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions
        val subscriptionIterator = subscriptions.iterator()

        while (subscriptionIterator.hasNext()) {
            val subscription = subscriptionIterator.next() as Subscription
            sqlDatabases.addAll(listSqlDatabasesBySubscriptionId(subscription.subscriptionId(), force))
        }

        return sqlDatabases
    }

    fun listSqlDatabases(): List<SqlDatabase> {
        val sqlDatabases = mutableListOf<SqlDatabase>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions
        val subscriptionIterator = subscriptions.iterator()

        while (subscriptionIterator.hasNext()) {
            val subscription = subscriptionIterator.next() as Subscription
            sqlDatabases.addAll(listSqlDatabasesBySubscriptionId(subscription.subscriptionId()))
        }

        return sqlDatabases
    }

    fun listDatabaseEditions(): List<DatabaseEditions> {
        val ret = mutableListOf<DatabaseEditions>()
        val editions = DatabaseEditions::class.java.declaredFields
        val editionsArraySize = editions.size

        for (editionIndex in 0 until editionsArraySize) {
            val field = editions[editionIndex]
            val modifier = field.modifiers
            if (Modifier.isPublic(modifier) && Modifier.isStatic(modifier) && Modifier.isFinal(modifier)) {
                val pt = field.get(null as Any?) as DatabaseEditions
                ret.add(pt)
            }
        }

        return ret
    }

    //endregion List

    fun getSqlServer(subscriptionId: String, sqlServerId: String): SqlServer {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().getById(sqlServerId)
    }

    /**
     * Create SQL Server from the configuration parameters
     */
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
                .withAdministratorPassword(sqlServerAdminPass.toString())
                .create()
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

    // TODO: Probably, move all to AsyncPromise instead of Observable
    fun getSqlServerAdminLoginAsync(database: SqlDatabase): Observable<String> {
        return Observable.fromCallable<String> {
            database.parent().manager().sqlServers()
                    .getByResourceGroup(database.resourceGroupName(), database.sqlServerName())
                    .administratorLogin()
        }.subscribeOn(SchedulerProviderFactory.getInstance().schedulerProvider.io())

    }

    //region Private Methods and Operators

    private fun listSqlDatabasesBySubscriptionId(subscriptionId: String): List<SqlDatabase> {
        return AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().list().flatMap { it.databases().list() }
    }

    private fun listSqlDatabasesBySubscriptionId(subscriptionId: String, force: Boolean): List<ResourceEx<SqlDatabase>> {

        if (!force && subscriptionIdToSqlDatabasesMap.containsKey(subscriptionId)) {
            return subscriptionIdToSqlDatabasesMap.getValue(subscriptionId)
        }

        val sqlDatabaseList = mutableListOf<ResourceEx<SqlDatabase>>()

        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val sqlServersIterator = azure.sqlServers().list().iterator()

            while (sqlServersIterator.hasNext()) {
                val sqlServer = sqlServersIterator.next() as SqlServer

                val sqlDatabaseIterator = sqlServer.databases().list().iterator()
                while (sqlDatabaseIterator.hasNext()) {
                    val sqlDatabase = sqlDatabaseIterator.next() as SqlDatabase
                    sqlDatabaseList.add(ResourceEx(sqlDatabase, subscriptionId))
                }
            }

            subscriptionIdToSqlDatabasesMap[subscriptionId] = sqlDatabaseList
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return sqlDatabaseList
    }

    //endregion Private Methods and Operators
}
