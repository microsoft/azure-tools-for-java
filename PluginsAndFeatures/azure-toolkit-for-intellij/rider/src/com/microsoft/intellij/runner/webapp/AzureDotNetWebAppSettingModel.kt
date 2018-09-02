package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.appservice.RuntimeStack

class AzureDotNetWebAppSettingModel {
    var subscriptionId: String = ""

    var isCreatingWebApp = false
    var webAppId: String = ""
    var webAppName: String = ""

    var isCreatingResourceGroup = false
    var resourceGroupName: String = ""

    var isCreatingAppServicePlan = false
    var appServicePlanId: String = ""
    var appServicePlanName: String = ""
    var operatingSystem: OperatingSystem? = null
    var location: String = ""
    var pricingTier: PricingTier? = null

    var runtime: RuntimeStack? = null

    var publishableProject: PublishableProjectModel? = null
}