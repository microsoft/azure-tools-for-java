package com.microsoft.intellij.runner.webapp

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.OperatingSystem
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.appservice.RuntimeStack
import com.microsoft.azure.management.sql.SqlDatabase

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
    var operatingSystem: OperatingSystem = defaultOperatingSystem
    var location: String = ""
    var pricingTier: PricingTier = defaultPricingTier

    var isDatabaseConnectionEnabled: Boolean = false
    var connectionStringName: String? = null
    var database: SqlDatabase? = null
    var sqlDatabaseAdminLogin: String? = null
    var sqlDatabaseAdminPassword = charArrayOf()

    var runtime: RuntimeStack = defaultRuntime

    var publishableProject: PublishableProjectModel? = null

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val defaultPricingTier = PricingTier.BASIC_B1!!
        val defaultRuntime = RuntimeStack("DOTNETCORE", "2.1")
    }
}