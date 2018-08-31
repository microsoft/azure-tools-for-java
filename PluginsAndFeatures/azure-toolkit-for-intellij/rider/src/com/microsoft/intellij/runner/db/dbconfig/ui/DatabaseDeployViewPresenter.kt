package com.microsoft.intellij.runner.db.dbconfig.ui

import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import rx.Observable

class DatabaseDeployViewPresenter<V : DatabaseDeployMvpView> : MvpPresenter<V>() {

    companion object {
        private const val CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions."
        private const val CANNOT_LIST_RES_GRP = "Failed to list resource groups."
        private const val CANNOT_LIST_SQL_SERVER = "Failed to list SQL Servers."
        private const val CANNOT_LIST_LOCATION = "Failed to list locations."
        private const val CANNOT_LIST_DATABASE_EDITION = "Failed to list database editions."
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
                }, { e -> errorHandler(CANNOT_LIST_RES_GRP, e as Exception) })
    }

    /**
     * Load SQL Servers from model.
     */
    fun onLoadSqlServers(subscriptionId: String, resourceGroupName: String) {
        Observable.fromCallable<List<SqlServer>> { AzureDatabaseMvpModel.getSqlServersBySubscriptionIdAndResourceGroup(subscriptionId, resourceGroupName) }
                .subscribeOn(schedulerProvider.io())
                .subscribe({ sqlServers ->
                    DefaultLoader.getIdeHelper().invokeLater {
                        if (isViewDetached) {
                            return@invokeLater
                        }
                        mvpView.fillSqlServer(sqlServers)
                    }
                }, { e -> errorHandler(CANNOT_LIST_SQL_SERVER, e as Exception) })
    }

    /**
     * Load locations from model.
     */
    fun onLoadLocation(sid: String) {
        Observable.fromCallable<List<Location>> { AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid) }
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
     * Load Database edition.
     */
    fun onLoadDatabaseEdition() {
        try {
            mvpView.fillDatabaseEdition(AzureDatabaseMvpModel.listDatabaseEditions())
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_DATABASE_EDITION, e)
        }

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
