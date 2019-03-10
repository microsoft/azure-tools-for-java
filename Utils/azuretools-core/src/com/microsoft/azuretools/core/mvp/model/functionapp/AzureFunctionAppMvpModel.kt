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

package com.microsoft.azuretools.core.mvp.model.functionapp

import com.microsoft.azure.management.appservice.FunctionApp
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

object AzureFunctionAppMvpModel {

    private val logger = Logger.getLogger(this::class.java.name)

    private val subscriptionIdToFunctionAppsMap = ConcurrentHashMap<String, List<FunctionApp>>()

    fun listAllFunctionApps(force: Boolean = false): List<ResourceEx<FunctionApp>> {
        if (!force && subscriptionIdToFunctionAppsMap.isNotEmpty())
            return subscriptionIdToFunctionAppsMap.flatMap { entry -> entry.value.map { function -> ResourceEx(function, entry.key) } }

        val functionAppsRes = ArrayList<ResourceEx<FunctionApp>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {

            val subscriptionId = subscription.subscriptionId()

            val functionApps = listFunctionAppsBySubscriptionId(subscriptionId, force)
            subscriptionIdToFunctionAppsMap[subscriptionId] = functionApps

            functionApps.forEach { function -> functionAppsRes.add(ResourceEx(function, subscriptionId)) }
        }

        return functionAppsRes
    }

    fun listFunctionAppsBySubscriptionId(subscriptionId: String, force: Boolean = false): List<FunctionApp> {
        if (!force && subscriptionIdToFunctionAppsMap.containsKey(subscriptionId)) {
            val functionApps = subscriptionIdToFunctionAppsMap[subscriptionId]
            if (functionApps != null) return functionApps
        }

        try {
            val functionApps = getAzureFunctionAppsBySubscriptionId(subscriptionId)
            subscriptionIdToFunctionAppsMap[subscriptionId] = functionApps
            return functionApps
        } catch (e: IOException) {
            logger.warning("Error getting Azure Function Apps by Subscription Id: $e")
        }

        return listOf()
    }

    fun getAzureFunctionAppsBySubscriptionId(subscriptionId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).appServices().functionApps()
                    .list() ?: emptyList<FunctionApp>()

    fun getAzureFunctionAppsByResourceGroup(subscriptionId: String, resourceGroupName: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).appServices().functionApps()
                    .listByResourceGroup(resourceGroupName)

    fun startFunctionApp(subscriptionId: String, functionAppId: String) {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        azure.appServices().functionApps().getById(functionAppId).start()
    }

    fun restartFunctionApp(subscriptionId: String, functionAppId: String) {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        azure.appServices().functionApps().getById(functionAppId).restart()
    }

    fun stopFunctionApp(subscriptionId: String, functionAppId: String) {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        azure.appServices().functionApps().getById(functionAppId).stop()
    }

    fun deleteFunctionApp(subscriptionId: String, functionAppId: String) {
        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            azure.appServices().functionApps().deleteById(functionAppId)
        } catch (t: Throwable) {
            logger.warning("Error deleting Azure Function App: $t")
            throw t
        }
    }

    fun clearSubscriptionIdToFunctionMap() {
        subscriptionIdToFunctionAppsMap.clear()
    }
}