package com.microsoft.intellij.runner.db.dbconfig.ui

import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.DatabaseEditions
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.ui.base.MvpView

interface DatabaseDeployMvpView : MvpView {

    fun fillSubscription(subscriptions: List<Subscription>)

    fun fillResourceGroup(resourceGroups: List<ResourceGroup>)

    fun fillSqlServer(sqlServers: List<SqlServer>)

    fun fillLocation(locations: List<Location>)

    fun fillDatabaseEdition(prices: List<DatabaseEditions>)
}