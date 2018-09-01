package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier

class AzureDotNetWebAppSettingModel {
    var subscriptionId: String = ""

    var publishableProject: PublishableProjectModel? = null

    var isCreatingWebApp = false
    var webAppId: String = ""
    var webAppName: String = ""

    var isCreatingResourceGroup = false
    var resourceGroupName: String = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId: String = ""
    var appServicePlanName: String = ""
    var operatingSystem: OperatingSystem = OperatingSystem.WINDOWS
    var location: String = ""
    var pricingTier: PricingTier = PricingTier.STANDARD_S1
}