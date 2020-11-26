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

package com.microsoft.azuretools.core.mvp.model.database

import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object AzureSqlServerMvpModel {

    private val logger = LoggerFactory.getLogger(AzureSqlServerMvpModel::class.java)

    private val subscriptionIdToSqlServersMap = ConcurrentHashMap<String, List<SqlServer>>()

    fun refreshSubscriptionToSqlServerMap() {
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {
            listSqlServersBySubscriptionId(subscription.subscriptionId(), true)
        }
    }

    fun getSqlServerById(subscriptionId: String, sqlServerId: String): SqlServer {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().getById(sqlServerId)
    }

    fun listSqlServersBySubscriptionId(subscriptionId: String, force: Boolean = false): List<SqlServer> {
        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            val sqlServers = subscriptionIdToSqlServersMap[subscriptionId]
            if (sqlServers != null) return sqlServers
        }

        try {
            val sqlServerList = getSqlServersBySubscriptionId(subscriptionId)
            subscriptionIdToSqlServersMap[subscriptionId] = sqlServerList
            return sqlServerList
        } catch (e: IOException) {
            logger.error(e.toString())
        }

        return listOf()
    }

    fun getSqlServerByName(subscriptionId: String,
                           name: String,
                           force: Boolean = false): SqlServer? {

        if (!force && subscriptionIdToSqlServersMap.containsKey(subscriptionId)) {
            val sqlServers = subscriptionIdToSqlServersMap[subscriptionId]
            if (sqlServers != null) {
                return sqlServers.find { it.name() == name }
            }
        }

        return listSqlServersBySubscriptionId(subscriptionId, true).find { it.name() == name }
    }

    fun listSqlServers(force: Boolean = false): List<ResourceEx<SqlServer>> {
        if (!force && subscriptionIdToSqlServersMap.isNotEmpty())
            return subscriptionIdToSqlServersMap.flatMap { idToList ->
                idToList.value.map { sqlServer -> ResourceEx(sqlServer, idToList.key) }
            }

        val sqlServers = ArrayList<ResourceEx<SqlServer>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {
            val subscriptionId = subscription.subscriptionId()
            val servers = listSqlServersBySubscriptionId(subscriptionId, force)
            subscriptionIdToSqlServersMap[subscriptionId] = servers
            sqlServers.addAll(servers.map { sqlServer -> ResourceEx(sqlServer, subscriptionId) })
        }

        return sqlServers
    }

    fun createSqlServer(subscriptionId: String,
                        sqlServerName: String,
                        region: Region,
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

    fun deleteSqlServer(subscriptionId: String, sqlServerId: String) {
        try {
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).sqlServers().deleteById(sqlServerId)
        } catch (e: Throwable) {
            logger.error(e.toString())
            throw e
        }
    }

    fun isNameAvailable(subscriptionId: String, name: String): Boolean {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.sqlServers().checkNameAvailability(name).isAvailable
    }

    fun clearSubscriptionIdToSqlServersMap() {
        subscriptionIdToSqlServersMap.clear()
    }

    @TestOnly
    fun setSubscriptionIdToSqlServersMap(map: Map<String, List<SqlServer>>) {
        subscriptionIdToSqlServersMap.clear()
        map.forEach { (subscriptionId, sqlServerList) ->
            subscriptionIdToSqlServersMap[subscriptionId] = sqlServerList
        }
    }

    private fun getSqlServersBySubscriptionId(subscriptionId: String): List<SqlServer> {
        val sqlServerList = ArrayList<SqlServer>()

        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val sqlServers = azure.sqlServers().list()

            for (sqlServer in sqlServers) {
                sqlServerList.add(sqlServer)
            }
        } catch (e: IOException) {
            logger.error(e.toString())
        }

        return sqlServerList
    }
}
