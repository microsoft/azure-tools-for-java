package com.microsoft.intellij.runner.webapp.webappconfig.ui

import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.RuntimeStack
import com.microsoft.azure.management.appservice.WebApp
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.ResourceEx
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
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
    }

    fun onRefresh() {
        loadWebApps(true)
    }

    fun onLoadWebApps() {
        loadWebApps(false)
    }

    /**
     * Load subscriptions from model.
     */
    fun onLoadSubscription() {
        Observable.fromCallable<List<Subscription>> { AzureMvpModel.getInstance().selectedSubscriptions }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ subscriptions ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.fillSubscription(subscriptions)
                    }
                }, { e -> errorHandler(CANNOT_LIST_SUBSCRIPTION, e as Exception) })
    }

    /**
     * Load resource groups from model.
     */
    fun onLoadResourceGroups(subscriptionId: String) {
        Observable.fromCallable<List<ResourceGroup>> { AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId) }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ resourceGroups ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.fillResourceGroup(resourceGroups)
                    }
                }, { e -> errorHandler(CANNOT_LIST_RESOURCE_GROUP, e as Exception) })
    }

    /**
     * Load app service plan from model.
     */
    fun onLoadAppServicePlan(subscriptionId: String) {
        Observable.fromCallable<List<AppServicePlan>> { AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(subscriptionId) }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ appServicePlans ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.fillAppServicePlan(appServicePlans)
                    }
                }, { e -> errorHandler(CANNOT_LIST_APP_SERVICE_PLAN, e as Exception) })
    }

    /**
     * Load App Service Plan Operating Systems from model.
     */
    fun onLoadOperatingSystem() {
        try {
            val operatingSystems = AzureDotNetWebAppMvpModel.listOperatingSystem()
            mvpView.fillOperatingSystem(operatingSystems)
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_OPERATING_SYSTEM, e)
        }
    }

    /**
     * Load locations from model.
     */
    fun onLoadLocation(subscriptionId: String) {
        Observable.fromCallable<List<Location>> { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId) }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ locations ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.fillLocation(locations)
                    }
                }, { e -> errorHandler(CANNOT_LIST_LOCATION, e as Exception) })
    }

    /**
     * Load pricing tier from model.
     */
    fun onLoadPricingTier() {
        try {
            mvpView.fillPricingTier(AzureMvpModel.getInstance().listPricingTier())
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_PRICING_TIER, e)
        }
    }

    /**
     * Load runtime from model.
     */
    fun onLoadRuntime() {
        try {
            val runtimeList = AzureDotNetWebAppMvpModel.listRuntimeStack().filter { it.stack().equals("DOTNETCORE", true) }
            val updatedRuntimeList = listOf(
                    *runtimeList.toTypedArray(),
                    RuntimeStack("DOTNETCORE", "2.0"),
                    RuntimeStack("DOTNETCORE", "2.1")
            ).sortedBy { it.version() }

            mvpView.fillRuntime(updatedRuntimeList)
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_RUNTIME_STACK, e)
        }
    }

    //region Private Methods and Operators

    private fun loadWebApps(forceRefresh: Boolean) {
        Observable.fromCallable<List<ResourceEx<WebApp>>> { AzureDotNetWebAppMvpModel.listWebApps(forceRefresh) }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ webAppList ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.renderWebAppsTable(webAppList)
                    }
                }, { e -> errorHandler(CANNOT_LIST_WEB_APP, e as Exception) })
    }

    private fun errorHandler(msg: String, e: Exception) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached) {
                return@invokeLater
            }
            mvpView.onErrorWithException(msg, e)
        }
    }

    //endregion Private Methods and Operators
}
