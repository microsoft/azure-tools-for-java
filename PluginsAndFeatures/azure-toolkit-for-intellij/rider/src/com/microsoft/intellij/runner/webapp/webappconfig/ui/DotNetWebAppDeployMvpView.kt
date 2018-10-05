package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.ui.base.MvpView

interface DotNetWebAppDeployMvpView : MvpView {

    fun renderWebAppsTable(webAppLists: List<ResourceEx<WebApp>>)

    fun fillSubscription(subscriptions: List<Subscription>)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>)

    fun fillAppServicePlan(appServicePlans: List<AppServicePlan>)

    fun fillLocation(locations: List<Location>)

    fun fillPricingTier(prices: List<PricingTier>)

    fun fillSqlDatabase(databases: List<SqlDatabase>)

    fun fillDatabaseEdition(prices: List<DatabaseEditions>)

    fun fillSqlServer(sqlServers: List<SqlServer>)

    fun fillPublishableProject(publishableProjects: List<PublishableProjectModel>)
}
