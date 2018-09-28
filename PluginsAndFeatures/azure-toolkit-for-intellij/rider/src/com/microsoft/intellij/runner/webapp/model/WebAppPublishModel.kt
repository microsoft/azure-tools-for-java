package com.microsoft.intellij.runner.webapp.model

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region

class WebAppPublishModel {

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val defaultPricingTier: PricingTier = PricingTier.STANDARD_S1
        val defaultLocation: String = Region.US_EAST.name()
        val defaultNetFrameworkVersion: NetFrameworkVersion = NetFrameworkVersion.fromString("4.7")
        val defaultRuntime = RuntimeStack("DOTNETCORE", "2.1")
    }

    var publishableProject: PublishableProjectModel? = null

    var subscription: Subscription? = null

    var isCreatingWebApp = false
    var webAppId = ""
    var webAppName = ""

    var isCreatingResourceGroup = false
    var resourceGroupName = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId = ""
    var appServicePlanName = ""
    var operatingSystem = defaultOperatingSystem
    var location = defaultLocation
    var pricingTier = defaultPricingTier

    var netFrameworkVersion = defaultNetFrameworkVersion
    var netCoreRuntime = defaultRuntime

    /**
     * Reset the model with values after creating a new instance
     */
    fun resetOnPublish(webApp: WebApp) {
        isCreatingWebApp = false
        webAppId = webApp.id()
        webAppName = ""

        isCreatingResourceGroup = false

        isCreatingAppServicePlan = false
        appServicePlanId = webApp.appServicePlanId()
        appServicePlanName = ""
    }
}