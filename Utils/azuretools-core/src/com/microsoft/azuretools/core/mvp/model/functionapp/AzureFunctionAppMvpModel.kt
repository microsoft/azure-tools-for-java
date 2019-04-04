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

import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.storage.SkuName
import com.microsoft.azure.management.storage.StorageAccount
import com.microsoft.azure.management.storage.StorageAccountSkuType
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.appserviceplan.AzureAppServicePlanMvpModel
import com.microsoft.azuretools.core.mvp.model.storage.AzureStorageAccountMvpModel
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

object AzureFunctionAppMvpModel {

    private val logger = Logger.getLogger(AzureFunctionAppMvpModel::class.java.name)

    private val subscriptionIdToFunctionAppsMap = ConcurrentHashMap<String, List<FunctionApp>>()
    private val appToConnectionStringsMap = ConcurrentHashMap<FunctionApp, List<ConnectionString>>()

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

    fun getFunctionAppById(subscriptionId: String, appId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).appServices().functionApps().getById(appId)

    fun startFunctionApp(subscriptionId: String, functionAppId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId)
                    .appServices().functionApps().getById(functionAppId).start()

    fun restartFunctionApp(subscriptionId: String, functionAppId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId)
                    .appServices().functionApps().getById(functionAppId).restart()

    fun stopFunctionApp(subscriptionId: String, functionAppId: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId)
                    .appServices().functionApps().getById(functionAppId).stop()

    fun deleteFunctionApp(subscriptionId: String, functionAppId: String) {
        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            azure.appServices().functionApps().deleteById(functionAppId)
        } catch (t: Throwable) {
            logger.warning("Error deleting Azure Function App: $t")
            throw t
        }
    }

    fun createFunctionApp(subscriptionId: String,
                          appName: String,
                          isCreateResourceGroup: Boolean,
                          resourceGroupName: String,
                          isCreateAppServicePlan: Boolean,
                          appServicePlanId: String,
                          appServicePlanName: String,
                          region: Region,
                          pricingTier: PricingTier,
                          isCreateStorageAccount: Boolean,
                          storageAccountId: String,
                          storageAccountName: String,
                          storageAccountType: StorageAccountSkuType): FunctionApp {

        val definition = createFunctionAppDefinition(subscriptionId, appName)

        val withPlan =
                if (isCreateAppServicePlan) {
                    val withRegion = definition.withRegion(region)

                    val withResourceGroup =
                            if (isCreateResourceGroup) withRegion.withNewResourceGroup(resourceGroupName)
                            else withRegion.withExistingResourceGroup(resourceGroupName)

                    val planWithRegion =
                            AuthMethodManager.getInstance().getAzureClient(subscriptionId).appServices().appServicePlans()
                                    .define(appServicePlanName)
                                    .withRegion(region)

                    val planWithResourceGroup =
                            if (isCreateResourceGroup) planWithRegion.withNewResourceGroup(resourceGroupName)
                            else planWithRegion.withExistingResourceGroup(resourceGroupName)

                    val planCreatable = planWithResourceGroup
                            .withPricingTier(pricingTier)
                            .withOperatingSystem(OperatingSystem.WINDOWS)

                    withResourceGroup.withNewAppServicePlan(planCreatable)
                } else {
                    val appServicePlan = AzureAppServicePlanMvpModel.getAppServicePlanById(subscriptionId, appServicePlanId)
                    val withPlan = definition.withExistingAppServicePlan(appServicePlan)

                    val withResourceGroup =
                            if (isCreateResourceGroup) withPlan.withNewResourceGroup(resourceGroupName)
                            else withPlan.withExistingResourceGroup(resourceGroupName)

                    withResourceGroup
                }

        val storageAccount = AzureStorageAccountMvpModel.getStorageAccountById(subscriptionId, storageAccountId)
        val withAppStorage = withStorageAccount(withPlan, isCreateStorageAccount, storageAccount, storageAccountName, storageAccountType.name())

        return withAppStorage
                .withLatestRuntimeVersion()
                .create()
    }

    fun checkFunctionAppNameExists(subscriptionId: String, nameToCheck: String, force: Boolean = false): Boolean {
        if (!force && subscriptionIdToFunctionAppsMap.containsKey(subscriptionId)) {
            return subscriptionIdToFunctionAppsMap[subscriptionId]!!.any { app -> app.name() == nameToCheck }
        }

        val functionApps = listAllFunctionApps(force = true)
        return functionApps.any { app -> app.resource.name() == nameToCheck }
    }

    fun getConnectionStrings(app: FunctionApp, force: Boolean): List<ConnectionString> {
        if (!force && appToConnectionStringsMap.containsKey(app)) {
            val connectionStrings = appToConnectionStringsMap[app]
            if (connectionStrings != null)
                return connectionStrings
        }

        val connectionStrings = app.connectionStrings.values.toList()
        appToConnectionStringsMap[app] = connectionStrings

        return connectionStrings
    }

    fun checkConnectionStringNameExists(subscriptionId: String, appId: String, connectionStringName: String, force: Boolean = false): Boolean {
        if (!force && subscriptionIdToFunctionAppsMap.containsKey(subscriptionId)) {
            val app = subscriptionIdToFunctionAppsMap[subscriptionId]!!.find { it.id() == appId } ?: return false
            return checkConnectionStringNameExists(app, connectionStringName, force)
        }

        val app = AzureFunctionAppMvpModel.getFunctionAppById(subscriptionId, appId)
        return checkConnectionStringNameExists(app, connectionStringName, force)
    }

    fun checkConnectionStringNameExists(app: FunctionApp, connectionStringName: String, force: Boolean = false): Boolean {
        if (!force) {
            if (!appToConnectionStringsMap.containsKey(app)) return false
            return appToConnectionStringsMap[app]?.any { it.name() == connectionStringName } ?: false
        }

        val connectionStrings = getConnectionStrings(app, true)
        return connectionStrings.any { it.name() == connectionStringName }
    }

    fun refreshSubscriptionToFunctionAppMap() {
        listAllFunctionApps(true)
    }

    fun clearSubscriptionIdToFunctionMap() {
        subscriptionIdToFunctionAppsMap.clear()
    }

    private fun createFunctionAppDefinition(subscriptionId: String, name: String) =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).appServices().functionApps().define(name)

    private fun withStorageAccount(definition: FunctionApp.DefinitionStages.WithCreate,
                                   isCreateNew: Boolean,
                                   storageAccount: StorageAccount?,
                                   name: String,
                                   skuName: SkuName): FunctionApp.DefinitionStages.WithCreate {

        return if (isCreateNew) definition.withNewStorageAccount(name, skuName)
        else definition.withExistingStorageAccount(storageAccount)
    }
}
