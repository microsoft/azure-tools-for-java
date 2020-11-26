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

package com.microsoft.intellij.runner.webapp

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.rd.util.concurrentMapOf
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.intellij.runner.webapp.model.WebAppPublishModel
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.azure.RiderAzureBundle

object AzureDotNetWebAppMvpModel {

    class WebAppDefinition(val name: String,
                           val isCreatingResourceGroup: Boolean,
                           val resourceGroupName: String)

    class AppServicePlanDefinition(val name: String,
                                   val pricingTier: PricingTier,
                                   val region: Region)

    private val logger = Logger.getInstance(AzureDotNetWebAppMvpModel::class.java)

    private val subscriptionIdToWebAppsMap = concurrentMapOf<String, List<ResourceEx<WebApp>>>()
    private val webAppToConnectionStringsMap = concurrentMapOf<WebApp, List<ConnectionString>>()
    private val webAppToDeploymentSlotMap = concurrentMapOf<WebApp, List<DeploymentSlot>>()

    //region Web App

    fun listWebApps(force: Boolean): List<ResourceEx<WebApp>> {
        val webAppList = arrayListOf<ResourceEx<WebApp>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        for (subscription in subscriptions) {
            webAppList.addAll(listWebAppsBySubscriptionId(subscription.subscriptionId(), force))
        }

        return webAppList
    }

    fun refreshSubscriptionToWebAppMap() {
        listWebApps(true)
    }

    fun clearSubscriptionToWebAppMap() {
        subscriptionIdToWebAppsMap.clear()
    }

    fun clearWebAppToConnectionStringsMap() {
        webAppToConnectionStringsMap.clear()
    }

    private fun listWebAppsBySubscriptionId(subscriptionId: String, force: Boolean): List<ResourceEx<WebApp>> {

        if (!force && subscriptionIdToWebAppsMap.containsKey(subscriptionId)) {
            val webApps = subscriptionIdToWebAppsMap[subscriptionId]
            if (webApps != null) return webApps
        }

        try {
            val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
            val webAppList = azure.webApps().list().map { ResourceEx(it, subscriptionId) }
            subscriptionIdToWebAppsMap[subscriptionId] = webAppList
            return webAppList
        } catch (e: Throwable) {
            logger.error(e)
        }

        return listOf()
    }

    fun createWebApp(subscriptionId: String,
                     appName: String,
                     isCreatingResourceGroup: Boolean,
                     resourceGroupName: String,
                     operatingSystem: OperatingSystem,
                     isCreatingAppServicePlan: Boolean,
                     appServicePlanId: String?,
                     appServicePlanName: String?,
                     pricingTier: PricingTier?,
                     location: Region?,
                     netFrameworkVersion: NetFrameworkVersion?,
                     netCoreRuntime: RuntimeStack?): WebApp {

        if (appName.isEmpty())
            throw RuntimeException(RiderAzureBundle.message("process_event.publish.web_apps.name_not_defined"))

        if (resourceGroupName.isEmpty())
            throw RuntimeException(RiderAzureBundle.message("process_event.publish.resource_group.not_defined"))

        val webAppDefinition = WebAppDefinition(appName, isCreatingResourceGroup, resourceGroupName)

        val windowsNetFramework = netFrameworkVersion ?: let {
            val version = WebAppPublishModel.defaultNetFrameworkVersion
            logger.info("Net Framework version is not provided. Use a default value: $version")
            version
        }

        val linuxDotNetRuntime = netCoreRuntime ?: let {
            val version =  WebAppPublishModel.defaultRuntime
            logger.info("Net Core Runtime version is not provided. Use a default value: $version")
            version
        }

        val webApp =
                if (isCreatingAppServicePlan) {
                    if (appServicePlanName.isNullOrEmpty())
                        throw RuntimeException(RiderAzureBundle.message("process_event.publish.service_plan.name_not_defined"))

                    if (pricingTier == null)
                        throw RuntimeException(RiderAzureBundle.message("process_event.publish.pricing_tier.not_defined"))

                    if (location == null)
                        throw RuntimeException(RiderAzureBundle.message("process_event.publish.location.not_defined"))

                    val appServicePlanDefinition = AppServicePlanDefinition(appServicePlanName, pricingTier, location)

                    if (operatingSystem == OperatingSystem.WINDOWS) {
                        createWebAppWithNewWindowsAppServicePlan(
                                subscriptionId,
                                webAppDefinition,
                                appServicePlanDefinition,
                                windowsNetFramework)
                    } else {
                        createWebAppWithNewLinuxAppServicePlan(
                                subscriptionId,
                                webAppDefinition,
                                appServicePlanDefinition,
                                linuxDotNetRuntime)
                    }
                } else {
                    if (appServicePlanId.isNullOrEmpty())
                        throw RuntimeException(RiderAzureBundle.message("process_event.publish.service_plan.not_defined"))

                    if (operatingSystem == OperatingSystem.WINDOWS) {

                        createWebAppWithExistingWindowsAppServicePlan(
                                subscriptionId,
                                webAppDefinition,
                                appServicePlanId,
                                windowsNetFramework)
                    } else {
                        createWebAppWithExistingLinuxAppServicePlan(
                                subscriptionId,
                                webAppDefinition,
                                appServicePlanId,
                                linuxDotNetRuntime)
                    }
                }

        logger.info("Created Web App with Name '${webApp.name()}' and Id '${webApp.id()}'.")

        return webApp
    }

    fun createWebAppWithNewWindowsAppServicePlan(subscriptionId: String,
                                                 webApp: WebAppDefinition,
                                                 appServicePlan: AppServicePlanDefinition,
                                                 netFrameworkVersion: NetFrameworkVersion): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)

        return webAppWithNewWindowsAppServicePlan(azure, webApp, appServicePlan)
                .withNetFrameworkVersion(netFrameworkVersion)
                .create()
    }

    fun createWebAppWithNewLinuxAppServicePlan(subscriptionId: String,
                                               webApp: WebAppDefinition,
                                               appServicePlan: AppServicePlanDefinition,
                                               runtime: RuntimeStack): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithNewLinuxAppServicePlan(azure, webApp, appServicePlan, runtime)
                .create()
    }

    fun createWebAppWithExistingWindowsAppServicePlan(subscriptionId: String,
                                                      webApp: WebAppDefinition,
                                                      appServicePlanId: String,
                                                      netFrameworkVersion: NetFrameworkVersion): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingWindowsAppServicePlan(azure, webApp, appServicePlanId)
                .withNetFrameworkVersion(netFrameworkVersion)
                .create()
    }

    fun createWebAppWithExistingLinuxAppServicePlan(subscriptionId: String,
                                                    webApp: WebAppDefinition,
                                                    appServicePlanId: String,
                                                    runtime: RuntimeStack): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingLinuxAppServicePlan(azure, webApp, appServicePlanId, runtime)
                .create()
    }

    fun getConnectionStrings(webApp: WebApp, force: Boolean = false): List<ConnectionString> {
        if (!force && webAppToConnectionStringsMap.containsKey(webApp)) {
            val connectionStrings = webAppToConnectionStringsMap[webApp]
            if (connectionStrings != null) return connectionStrings
        }

        val connectionStrings = webApp.connectionStrings.values.toList()
        webAppToConnectionStringsMap[webApp] = connectionStrings

        return connectionStrings
    }

    fun checkConnectionStringNameExists(webApp: WebApp, connectionStringName: String, force: Boolean = false): Boolean {
        if (!force) {
            if (!webAppToConnectionStringsMap.containsKey(webApp)) return false
            return webAppToConnectionStringsMap.getValue(webApp).any { it.name() == connectionStringName }
        }

        val connectionStrings = getConnectionStrings(webApp, true)
        return connectionStrings.any { it.name() == connectionStringName }
    }

    fun getWebAppByName(subscriptionId: String,
                        resourceGroupName: String,
                        webAppName: String,
                        force: Boolean = false): WebApp? {

        val webAppFilter: (WebApp) -> Boolean = { webApp ->
            webApp.resourceGroupName() == resourceGroupName && webApp.name() == webAppName
        }

        if (!force && subscriptionIdToWebAppsMap.containsKey(subscriptionId)) {
            val webAppResources = subscriptionIdToWebAppsMap[subscriptionId]
            if (webAppResources != null) {
                return webAppResources.map { it.resource }.find { webAppFilter(it) }
            }
        }

        return listWebAppsBySubscriptionId(subscriptionId, force)
                .map { it.resource }
                .find { webAppFilter(it) }
    }

    fun getWebAppById(subscriptionId: String, appId: String): WebApp =
            AuthMethodManager.getInstance().getAzureClient(subscriptionId).webApps().getById(appId)

    //endregion Web App

    //region Check Existence

    /**
     * Check an Azure Resource Group name existence over azure portal
     *
     * Note: Method should be used in configuration validation logic.
     *       Suppress for now, because current configuration validation mechanism does not allow to easily make async call for validation
     */
    @Suppress("unused")
    fun checkResourceGroupExistence(subscriptionId: String, resourceGroupName: String): Boolean {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return azure.resourceGroups().contain(resourceGroupName)
    }

    //endregion Check Existence

    //region Deployment Slot

    fun listDeploymentSlots(app: WebApp, force: Boolean = false): List<DeploymentSlot> {
        if (!force && webAppToDeploymentSlotMap.containsKey(app)) {
            return webAppToDeploymentSlotMap.getValue(app)
        }

        try {
            val slots = app.deploymentSlots().list()

            if (logger.isTraceEnabled)
                logger.trace("Found ${slots.size} slot(s) for app '${app.name()}'.")

            webAppToDeploymentSlotMap[app] = slots
            return slots
        } catch (t: Throwable) {
            logger.error("Error on getting Deployment Slots for function app '${app.name()}': $t")
        }

        return emptyList()
    }

    fun checkDeploymentSlotExists(app: WebApp, name: String, force: Boolean = false): Boolean {
        if (!force) {
            return webAppToDeploymentSlotMap[app]?.any { it.name() == name } == true
        }

        return app.deploymentSlots().list().any { it.name() == name }
    }

    fun createDeploymentSlot(subscriptionId: String, appId: String, name: String): DeploymentSlot {
        val app = getWebAppById(subscriptionId, appId)
        return createDeploymentSlot(app, name)
    }

    fun createDeploymentSlot(app: WebApp, name: String, source: String? = null): DeploymentSlot {
        val definedSlot = app.deploymentSlots().define(name)

        if (source == null)
            return definedSlot.withBrandNewConfiguration().create()

        if (source == app.name())
            return definedSlot.withConfigurationFromParent().create()

        val configurationSourceSlot = app.deploymentSlots().list().find { slot -> source == slot.name() }
                ?: throw IllegalStateException("Unable to find source configuration '$source' for deployment slot.")

        return definedSlot.withConfigurationFromDeploymentSlot(configurationSourceSlot).create()
    }

    //endregion Deployment Slot

    @TestOnly
    fun setWebAppToConnectionStringsMap(map: Map<WebApp, List<ConnectionString>>) {
        webAppToConnectionStringsMap.clear()
        map.forEach { (webApp, connectionStringList) ->
            webAppToConnectionStringsMap[webApp] = connectionStringList
        }
    }

    @TestOnly
    fun setWebAppToDeploymentSlotsMap(map: Map<WebApp, List<DeploymentSlot>>) {
        webAppToDeploymentSlotMap.clear()
        map.forEach { (webApp, deploymentSlotList) ->
            webAppToDeploymentSlotMap[webApp] = deploymentSlotList
        }
    }

    //region Private Methods and Operators

    private fun webAppWithNewWindowsAppServicePlan(azure: Azure,
                                                   webApp: WebAppDefinition,
                                                   appServicePlan: AppServicePlanDefinition) : WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlan.name, appServicePlan.region, appServicePlan.pricingTier, webApp.isCreatingResourceGroup, webApp.resourceGroupName, OperatingSystem.WINDOWS)

        val appWithRegion = azure.webApps().define(webApp.name).withRegion(appServicePlan.region)

        val appWithGroup =
                if (webApp.isCreatingResourceGroup) appWithRegion.withNewResourceGroup(webApp.resourceGroupName)
                else appWithRegion.withExistingResourceGroup(webApp.resourceGroupName)

        return appWithGroup.withNewWindowsPlan(withAppServicePlan)
    }

    private fun webAppWithNewLinuxAppServicePlan(azure: Azure,
                                                 webApp: WebAppDefinition,
                                                 appServicePlan: AppServicePlanDefinition,
                                                 runtime: RuntimeStack): WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlan.name, appServicePlan.region, appServicePlan.pricingTier, webApp.isCreatingResourceGroup, webApp.resourceGroupName, OperatingSystem.LINUX)

        val appWithRegion = azure.webApps().define(webApp.name).withRegion(appServicePlan.region)

        val appWithGroup =
                if (webApp.isCreatingResourceGroup) appWithRegion.withNewResourceGroup(webApp.resourceGroupName)
                else appWithRegion.withExistingResourceGroup(webApp.resourceGroupName)

        return appWithGroup.withNewLinuxPlan(withAppServicePlan).withBuiltInImage(runtime)
    }

    private fun withCreateAppServicePlan(azure: Azure,
                                         appServicePlanName: String,
                                         region: Region,
                                         pricingTier: PricingTier,
                                         isCreatingResourceGroup: Boolean,
                                         resourceGroupName: String,
                                         operatingSystem: OperatingSystem): AppServicePlan.DefinitionStages.WithCreate {
        val planWithRegion = azure.appServices().appServicePlans()
                .define(appServicePlanName)
                .withRegion(region)

        val planWithGroup =
                if (isCreatingResourceGroup) planWithRegion.withNewResourceGroup(resourceGroupName)
                else planWithRegion.withExistingResourceGroup(resourceGroupName)

        return planWithGroup
                .withPricingTier(pricingTier)
                .withOperatingSystem(operatingSystem)
    }

    private fun webAppWithExistingWindowsAppServicePlan(azure: Azure,
                                                        webApp: WebAppDefinition,
                                                        appServicePlanId: String) : WebApp.DefinitionStages.WithCreate {

        val windowsPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingWindowsPlan = azure.webApps().define(webApp.name).withExistingWindowsPlan(windowsPlan)

        return if (webApp.isCreatingResourceGroup) withExistingWindowsPlan.withNewResourceGroup(webApp.resourceGroupName)
        else withExistingWindowsPlan.withExistingResourceGroup(webApp.resourceGroupName)
    }

    private fun webAppWithExistingLinuxAppServicePlan(azure: Azure,
                                                      webApp: WebAppDefinition,
                                                      appServicePlanId: String,
                                                      runtime: RuntimeStack) : WebApp.DefinitionStages.WithCreate {

        val linuxPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingLinuxPlan = azure.webApps().define(webApp.name).withExistingLinuxPlan(linuxPlan)

        val withResourceGroup =
                if (webApp.isCreatingResourceGroup) withExistingLinuxPlan.withNewResourceGroup(webApp.resourceGroupName)
                else withExistingLinuxPlan.withExistingResourceGroup(webApp.resourceGroupName)

        return withResourceGroup.withBuiltInImage(runtime)
    }

    private fun withExistingAppServicePlan(azure: Azure, appServicePlanId: String): AppServicePlan {
        return azure.appServices().appServicePlans().getById(appServicePlanId)
    }

    //endregion Private Methods and Operators
}
