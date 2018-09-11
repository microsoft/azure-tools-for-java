package com.microsoft.intellij.runner.db.dbconfig.ui

import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter
import com.microsoft.intellij.runner.db.AzureDatabaseMvpModel
import com.microsoft.tooling.msservices.components.DefaultLoader
import rx.Observable

class DatabaseDeployViewPresenter<V : DatabaseDeployMvpView> : MvpPresenter<V>() {

    companion object {
        private const val CANNOT_LIST_SUBSCRIPTION = "Failed to list subscriptions."
        private const val CANNOT_LIST_RESOURCE_GROUP = "Failed to list resource groups."
        private const val CANNOT_LIST_SQL_SERVER = "Failed to list SQL Servers."
        private const val CANNOT_LIST_LOCATION = "Failed to list locations."
        private const val CANNOT_LIST_DATABASE_EDITION = "Failed to list database editions."
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

    fun onLoadSqlServers(subscriptionId: String) {
        subscribe(
                { AzureDatabaseMvpModel.listSqlServersBySubscriptionId(subscriptionId, true).map { it.resource } },
                { mvpView.fillSqlServer(it) },
                CANNOT_LIST_SQL_SERVER)
    }

    fun onLoadLocation(sid: String) {
        subscribe(
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(sid) },
                { mvpView.fillLocation(it) },
                CANNOT_LIST_LOCATION)
    }

    fun onLoadDatabaseEdition() {
        try {
            mvpView.fillDatabaseEdition(AzureDatabaseMvpModel.listDatabaseEditions())
        } catch (e: IllegalAccessException) {
            errorHandler(CANNOT_LIST_DATABASE_EDITION, e)
        }

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
