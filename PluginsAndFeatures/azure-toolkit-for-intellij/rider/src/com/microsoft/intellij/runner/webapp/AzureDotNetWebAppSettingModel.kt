package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.PricingTier

class AzureDotNetWebAppSettingModel {
    var subscriptionId: String? = null

    var publishableProject: PublishableProjectModel? = null

    var isCreatingWebApp = false
    var webAppId: String? = null
    var webAppName: String? = null

    var isCreatingResourceGroup = false
    var resourceGroupName: String? = null

    var isCreatingAppServicePlan = false
    var appServicePlanId: String? = null
    var appServicePlanName: String? = null
    var region: String? = null
    var pricingTier: PricingTier? = null
}