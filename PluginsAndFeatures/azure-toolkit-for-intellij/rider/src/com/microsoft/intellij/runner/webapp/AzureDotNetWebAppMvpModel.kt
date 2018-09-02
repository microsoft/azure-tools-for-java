package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.util.concurrentMapOf
import com.microsoft.azure.management.Azure
import com.microsoft.azure.management.appservice.*
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import java.io.IOException
import java.lang.reflect.Modifier

object AzureDotNetWebAppMvpModel {

    //region Web App

    private val subscriptionIdToWebAppsMap = concurrentMapOf<String, List<ResourceEx<WebApp>>>()

    fun listWebApps(force: Boolean): List<ResourceEx<WebApp>> {
        val webAppList = arrayListOf<ResourceEx<WebApp>>()
        val subscriptions = AzureMvpModel.getInstance().selectedSubscriptions

        subscriptions.forEach {
            webAppList.addAll(listWebAppsBySubscriptionId(it.subscriptionId(), force))
        }

        return webAppList
    }

    fun listWebApps(operatingSystem: OperatingSystem, force: Boolean): List<ResourceEx<WebApp>> {
        return listWebApps(force).filter { it.resource.operatingSystem() == operatingSystem }
    }

    fun cleanWebApps() {
        subscriptionIdToWebAppsMap.clear()
    }

    private fun listWebAppsBySubscriptionId(subscriptionId: String, force: Boolean): List<ResourceEx<WebApp>> {

        if (!force && subscriptionIdToWebAppsMap.containsKey(subscriptionId)) {
            return subscriptionIdToWebAppsMap.getValue(subscriptionId)
        } else {
            val webAppList = arrayListOf<ResourceEx<WebApp>>()

            try {
                val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
                val webApps = azure.webApps().list()

                webApps.forEach {
                    webAppList.add(ResourceEx(it, subscriptionId))
                }

                subscriptionIdToWebAppsMap[subscriptionId] = webAppList
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return webAppList
        }
    }

    fun createWebAppWithNewWindowsAppServicePlan(subscriptionId: String,
                                                 webAppName: String,
                                                 appServicePlanName: String,
                                                 pricingTier: PricingTier,
                                                 region: String,
                                                 isCreatingResourceGroup: Boolean,
                                                 resourceGroupName: String): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithNewWindowsAppServicePlan(
                azure,
                webAppName,
                appServicePlanName,
                pricingTier,
                region,
                isCreatingResourceGroup,
                resourceGroupName).create()
    }

    fun createWebAppWithNewLinuxAppServicePlan(subscriptionId: String,
                                               webAppName: String,
                                               appServicePlanName: String,
                                               pricingTier: PricingTier,
                                               region: String,
                                               runtime: RuntimeStack,
                                               isCreatingResourceGroup: Boolean,
                                               resourceGroupName: String): WebApp {
        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithNewLinuxAppServicePlan(
                azure,
                webAppName,
                appServicePlanName,
                pricingTier,
                region,
                runtime,
                isCreatingResourceGroup,
                resourceGroupName)
                .create()
    }

    fun createWebAppWithExistingWindowsAppServicePlan(subscriptionId: String,
                                                      webAppName: String,
                                                      appServicePlanId: String,
                                                      isCreatingResourceGroup: Boolean,
                                                      resourceGroupName: String): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingWindowsAppServicePlan(azure, webAppName, appServicePlanId, isCreatingResourceGroup, resourceGroupName)
                .create()
    }

    fun createWebAppWithExistingLinuxAppServicePlan(subscriptionId: String,
                                                    webAppName: String,
                                                    appServicePlanId: String,
                                                    runtime: RuntimeStack,
                                                    isCreatingResourceGroup: Boolean,
                                                    resourceGroupName: String): WebApp {

        val azure = AuthMethodManager.getInstance().getAzureClient(subscriptionId)
        return webAppWithExistingLinuxAppServicePlan(azure, webAppName, appServicePlanId, runtime, isCreatingResourceGroup, resourceGroupName)
                .create()
    }

    fun getDotNetWebApp(subscriptionId: String, webAppId: String): WebApp {
        return AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId)
    }

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

    //region Operating System

    fun listOperatingSystem(): List<OperatingSystem> {
        return OperatingSystem::class.java.declaredFields
                .filter { Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) }
                .map { it.get(null as Any?) as OperatingSystem }
                .toList()
    }

    //endregion Operating System

    //region Runtime Stack

    fun listRuntimeStack(): List<RuntimeStack> {
        return RuntimeStack::class.java.declaredFields
                .filter { Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers) && Modifier.isFinal(it.modifiers) }
                .map { it.get(null as Any?) as RuntimeStack }
                .toList()
    }

    //endregion Runtime Stack

    //region Private Methods and Operators

    private fun webAppWithNewWindowsAppServicePlan(azure: Azure,
                                                   webAppName: String,
                                                   appServicePlanName: String,
                                                   pricingTier: PricingTier,
                                                   region: String,
                                                   isCreatingResourceGroup: Boolean,
                                                   resourceGroupName: String) : WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlanName, region, pricingTier, isCreatingResourceGroup, resourceGroupName, OperatingSystem.WINDOWS)

        val appWithRegion = azure.webApps().define(webAppName).withRegion(region)

        val appWithGroup =
                if (isCreatingResourceGroup) appWithRegion.withNewResourceGroup(resourceGroupName)
                else appWithRegion.withExistingResourceGroup(resourceGroupName)

        return appWithGroup.withNewWindowsPlan(withAppServicePlan)
    }

    private fun webAppWithNewLinuxAppServicePlan(azure: Azure,
                                                 webAppName: String,
                                                 appServicePlanName: String,
                                                 pricingTier: PricingTier,
                                                 region: String,
                                                 runtime: RuntimeStack,
                                                 isCreatingResourceGroup: Boolean,
                                                 resourceGroupName: String): WebApp.DefinitionStages.WithCreate {

        val withAppServicePlan = withCreateAppServicePlan(
                azure, appServicePlanName, region, pricingTier, isCreatingResourceGroup, resourceGroupName, OperatingSystem.LINUX)

        val appWithRegion = azure.webApps().define(webAppName).withRegion(region)

        val appWithGroup =
                if (isCreatingResourceGroup) appWithRegion.withNewResourceGroup(resourceGroupName)
                else appWithRegion.withExistingResourceGroup(resourceGroupName)

        return appWithGroup.withNewLinuxPlan(withAppServicePlan).withBuiltInImage(runtime)
    }

    private fun withCreateAppServicePlan(azure: Azure,
                                         appServicePlanName: String,
                                         region: String,
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
                                                        webAppName: String,
                                                        appServicePlanId: String,
                                                        isCreatingResourceGroup: Boolean,
                                                        resourceGroupName: String) : WebApp.DefinitionStages.WithCreate {

        val windowsPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingWindowsPlan = azure.webApps().define(webAppName).withExistingWindowsPlan(windowsPlan)

        return if (isCreatingResourceGroup) withExistingWindowsPlan.withNewResourceGroup(resourceGroupName)
        else withExistingWindowsPlan.withExistingResourceGroup(resourceGroupName)
    }

    private fun webAppWithExistingLinuxAppServicePlan(azure: Azure,
                                                      webAppName: String,
                                                      appServicePlanId: String,
                                                      runtime: RuntimeStack,
                                                      isCreatingResourceGroup: Boolean,
                                                      resourceGroupName: String) : WebApp.DefinitionStages.WithCreate {

        val linuxPlan = withExistingAppServicePlan(azure, appServicePlanId)

        val withExistingLinuxPlan = azure.webApps().define(webAppName).withExistingLinuxPlan(linuxPlan)

        val withResourceGroup =
                if (isCreatingResourceGroup) withExistingLinuxPlan.withNewResourceGroup(resourceGroupName)
                else withExistingLinuxPlan.withExistingResourceGroup(resourceGroupName)

        return withResourceGroup.withBuiltInImage(runtime)
    }

    private fun withExistingAppServicePlan(azure: Azure,
                                           appServicePlanId: String): AppServicePlan {
        return azure.appServices().appServicePlans().getById(appServicePlanId)
    }



    //endregion Private Methods and Operators
}
