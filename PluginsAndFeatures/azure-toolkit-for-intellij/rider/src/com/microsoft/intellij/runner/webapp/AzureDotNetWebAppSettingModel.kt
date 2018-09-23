package com.microsoft.intellij.runner.webapp

import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase

class AzureDotNetWebAppSettingModel {

    companion object {
        val defaultOperatingSystem = OperatingSystem.WINDOWS
        val defaultPricingTier: PricingTier = PricingTier.STANDARD_S1
        val defaultLocation: String = Region.US_EAST.name()
        val defaultNetFrameworkVersion: NetFrameworkVersion = NetFrameworkVersion.fromString("4.7")
        val defaultRuntime = RuntimeStack("DOTNETCORE", "2.1")

        const val defaultCollation = "SQL_Latin1_General_CP1_CI_AS"
        val defaultDatabaseEditions: DatabaseEditions = DatabaseEditions.STANDARD
    }

    val webAppModel = WebAppModel()
    val databaseModel = DatabaseModel()

    class WebAppModel {

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

        var isOpenBrowser = false

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

    class DatabaseModel {

        var subscription: Subscription? = null

        var isDatabaseConnectionEnabled = false

        var connectionStringName = ""
        var database: SqlDatabase? = null

        var isCreatingSqlDatabase = false

        var databaseName = ""

        var isCreatingDbResourceGroup = true
        var dbResourceGroupName = ""

        var isCreatingSqlServer = true
        var sqlServerId = ""
        var sqlServerName = ""
        var sqlServerAdminLogin = ""

        @get:Transient
        var sqlServerAdminPassword = charArrayOf()

        @get:Transient
        var sqlServerAdminPasswordConfirm = charArrayOf()

        var sqlServerLocation = defaultLocation
        var databaseEdition = defaultDatabaseEditions

        var collation = defaultCollation

        /**
         * Reset the model with values after creating a new instance
         */
        fun resetOnPublish(sqlDatabase: SqlDatabase) {
            isDatabaseConnectionEnabled = true

            isCreatingSqlDatabase = false
            database = sqlDatabase
        }
    }
}