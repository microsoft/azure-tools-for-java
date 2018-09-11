package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.microsoft.azure.management.appservice.RuntimeStack
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
        private const val CANNOT_LIST_OPERATING_SYSTEM = "Failed to list operating system."
        private const val CANNOT_LIST_LOCATION = "Failed to list locations."
        private const val CANNOT_LIST_PRICING_TIER = "Failed to list pricing tier."
        private const val CANNOT_LIST_RUNTIME_STACK = "Failed to list runtime stack."
        private const val CANNOT_LIST_SQL_DATABASE = "Failed to list SQL Database."
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

    fun onLoadOperatingSystem() {
        try {
            val operatingSystems = AzureDotNetWebAppMvpModel.listOperatingSystem()
            mvpView.fillOperatingSystem(operatingSystems)
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_OPERATING_SYSTEM, e)
        }
    }

    fun onLoadLocation(subscriptionId: String) {
        subscribe(
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId) },
                { mvpView.fillLocation(it) },
                CANNOT_LIST_LOCATION)
    }

    fun onLoadPricingTier() {
        try {
            mvpView.fillPricingTier(AzureMvpModel.getInstance().listPricingTier())
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_PRICING_TIER, e)
        }
    }

    fun onLoadRuntime() {
        try {
            val runtimeList = AzureDotNetWebAppMvpModel.listRuntimeStack().filter { it.stack().equals("DOTNETCORE", true) }
            val updatedRuntimeList = listOf(
                    *runtimeList.toTypedArray(),
                    RuntimeStack("DOTNETCORE", "2.0"),
                    RuntimeStack("DOTNETCORE", "2.1")
            ).distinctBy { it.stack() + it.version() }
             .sortedBy { it.version() }

            mvpView.fillRuntime(updatedRuntimeList)
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_RUNTIME_STACK, e)
        }
    }

    fun onLoadSqlDatabase(subscriptionId: String) {
        subscribe(
                { AzureDatabaseMvpModel.listSqlDatabasesBySubscriptionId(subscriptionId).filter { it.name() != "master" } },
                { mvpView.fillSqlDatabase(it) },
                CANNOT_LIST_SQL_DATABASE)
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
