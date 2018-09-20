package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.util.idea.lifetime
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import rx.Observable

class DotNetWebAppDeployViewPresenter<V : DotNetWebAppDeployMvpView> : MvpPresenter<V>() {

    companion object {
        private const val CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions."
        private const val CANNOT_LIST_WEB_APP = "Failed to list web apps."
        private const val CANNOT_LIST_RESOURCE_GROUP = "Failed to list resource groups."
        private const val CANNOT_LIST_APP_SERVICE_PLAN = "Failed to list app service plan."
        private const val CANNOT_LIST_LOCATION = "Failed to list locations."
        private const val CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier."
        private const val CANNOT_LIST_SQL_DATABASE = "Failed to list SQL Database."
        private const val CANNOT_LIST_SQL_SERVER = "Failed to list SQL Server."
        private const val CANNOT_LIST_DATABASE_EDITION = "Failed to list SQL Database edition."
        private const val CANNOT_LIST_PUBLISHABLE_PROJECTS = "Failed to list publishable projects."
    }

    fun onRefresh() {
        loadWebApps(true)
    }

    fun onLoadWebApps() {
        loadWebApps(false)
    }

    fun onLoadSubscription() {
        subscribe(
                { AzureMvpModel.getInstance().selectedSubscriptions },
                { mvpView.fillSubscription(it) },
                CANNOT_LIST_SUBSCRIPTION)
    }

    fun onLoadResourceGroups(subscriptionId: String) {
        subscribe(
                { AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId) },
                { mvpView.fillResourceGroup(it) },
                CANNOT_LIST_RESOURCE_GROUP)
    }

    fun onLoadAppServicePlan(subscriptionId: String) {
        subscribe(
                { AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(subscriptionId) },
                { mvpView.fillAppServicePlan(it) },
                CANNOT_LIST_APP_SERVICE_PLAN)
    }

    fun onLoadLocation(subscriptionId: String) {
        subscribe(
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId) },
                { mvpView.fillLocation(it) },
                CANNOT_LIST_LOCATION)
    }

    fun onLoadPricingTier() {
        try {
            val pricingTiers = AzureMvpModel.getInstance().listPricingTier()
            mvpView.fillPricingTier(pricingTiers)
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_PRICING_TIER, e)
        }
    }

    fun onLoadSqlDatabase(subscriptionId: String) {
        subscribe(
                { AzureDatabaseMvpModel.listSqlDatabasesBySubscriptionId(subscriptionId).filter { it.name() != "master" } },
                { mvpView.fillSqlDatabase(it) },
                CANNOT_LIST_SQL_DATABASE)
    }

    fun onLoadSqlServers(subscriptionId: String) {
        subscribe(
                { AzureDatabaseMvpModel.listSqlServersBySubscriptionId(subscriptionId, true).map { it.resource } },
                { mvpView.fillSqlServer(it) },
                CANNOT_LIST_SQL_SERVER)
    }

    fun onLoadDatabaseEdition() {
        try {
            mvpView.fillDatabaseEdition(AzureDatabaseMvpModel.listDatabaseEditions())
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_DATABASE_EDITION, e)
        }

    }

    fun onLoadPublishableProjects(project: Project) {
        project.solution.publishableProjectsModel.publishableProjects.advise(project.lifetime.createNested()) {
            if (it.newValueOpt != null) {
                try {
                    mvpView.fillPublishableProject(project.solution.publishableProjectsModel.publishableProjects.values.toList())
                } catch (e: Exception) {
                    errorHandler(CANNOT_LIST_PUBLISHABLE_PROJECTS, e)
                }
            }
        }
    }

    private fun loadWebApps(forceRefresh: Boolean) {
        subscribe(
                { AzureDotNetWebAppMvpModel.listWebApps(forceRefresh) },
                { mvpView.renderWebAppsTable(it) },
                CANNOT_LIST_WEB_APP)
    }

    private fun <T>subscribe(callableFunc: () -> T, invokeLaterCallback: (T) -> Unit, msg: String) {
        Observable.fromCallable<T> { callableFunc() }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ values ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        invokeLaterCallback(values)
                    }
                }, { e -> errorHandler(msg, e as Exception) })
    }

    private fun errorHandler(msg: String, e: Exception) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached) {
                return@invokeLater
            }
            mvpView.onErrorWithException(msg, e)
        }
    }
}
