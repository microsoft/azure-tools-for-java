/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.runner.appbase.config.ui

import com.intellij.openapi.project.Project
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.isAlive
import com.jetbrains.rd.util.reactive.Signal
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azure.management.sql.SqlDatabase
import com.microsoft.azure.management.sql.SqlServer
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlDatabaseMvpModel
import com.microsoft.azuretools.core.mvp.model.database.AzureSqlServerMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.helpers.base.AzureMvpPresenter
import com.microsoft.tooling.msservices.components.DefaultLoader
import org.jetbrains.plugins.azure.RiderAzureBundle.message

abstract class AppDeployViewPresenterBase<T : AppDeployMvpViewBase> : AzureMvpPresenter<T>() {

    private val subscriptionSignal = Signal<List<Subscription>>()
    private val resourceGroupSignal = Signal<List<ResourceGroup>>()
    private val appServicePlanSignal = Signal<List<AppServicePlan>>()
    private val pricingTierSignal = Signal<List<PricingTier>>()
    private val locationSignal = Signal<List<Location>>()
    private val sqlDatabaseSignal = Signal<List<SqlDatabase>>()
    private val sqlServerSignal = Signal<List<SqlServer>>()

    abstract fun loadApps(lifetime: Lifetime, forceRefresh: Boolean)

    fun onRefresh(lifetime: Lifetime) {
        loadApps(lifetime, true)
    }

    fun onLoadApps(lifetime: Lifetime) {
        loadApps(lifetime, false)
    }

    fun onLoadSubscription(lifetime: Lifetime) {
        subscribe(lifetime,
                subscriptionSignal,
                message("progress.publish.subscription.collect"),
                message("run_config.publish.subscription.collect_error"),
                { AzureMvpModel.getInstance().selectedSubscriptions },
                { mvpView.fillSubscription(it) })
    }

    fun onLoadResourceGroups(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime,
                resourceGroupSignal,
                message("progress.publish.resource_group.collect"),
                message("run_config.publish.resource_group.collect_error"),
                { AzureMvpModel.getInstance().getResourceGroupsBySubscriptionId(subscriptionId) },
                { mvpView.fillResourceGroup(it) })
    }

    fun onLoadAppServicePlan(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime,
                appServicePlanSignal,
                message("progress.publish.service_plan.collect"),
                message("run_config.publish.service_plan.collect_error"),
                { AzureWebAppMvpModel.getInstance().listAppServicePlanBySubscriptionId(subscriptionId) },
                { mvpView.fillAppServicePlan(it) })
    }

    fun onLoadLocation(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime,
                locationSignal,
                message("progress.publish.location.collect"),
                message("run_config.publish.location.collect_error"),
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId) },
                { mvpView.fillLocation(it) })
    }

    fun onLoadPricingTier(lifetime: Lifetime) {
        subscribe(lifetime,
                pricingTierSignal,
                message("progress.publish.pricing_tier.collect"),
                message("run_config.publish.pricing_tier.collect_error"),
                { AzureMvpModel.getInstance().listPricingTier() },
                { mvpView.fillPricingTier(it) })
    }

    fun onLoadSqlDatabase(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime,
                sqlDatabaseSignal,
                message("progress.publish.sql_db.collect"),
                message("run_config.publish.sql_db.collect_error"),
                {
                    AzureSqlDatabaseMvpModel.listSqlDatabasesBySubscriptionId(subscriptionId)
                            .filter { it.resource.name() != "master" }
                            .map { it.resource }
                },
                { mvpView.fillSqlDatabase(it) })
    }

    fun onLoadSqlServers(lifetime: Lifetime, subscriptionId: String) {
        subscribe(lifetime,
                sqlServerSignal,
                message("progress.publish.sql_server.collect"),
                message("run_config.publish.sql_server.collect_error"),
                { AzureSqlServerMvpModel.listSqlServersBySubscriptionId(subscriptionId, true).map { it.resource } },
                { mvpView.fillSqlServer(it) })
    }

    fun onLoadPublishableProjects(lifetime: Lifetime, project: Project) {
        project.solution.publishableProjectsModel.publishableProjects.advise(lifetime) {
            if (it.newValueOpt != null) {
                application.invokeLater {
                    if (!lifetime.isAlive) return@invokeLater
                    try {
                        mvpView.fillPublishableProject(project.solution.publishableProjectsModel.publishableProjects.values.toList())
                    } catch (t: Throwable) {
                        errorHandler(message("run_config.publish.project.collect_error"), t)
                    }
                }
            }
        }
    }

    private fun errorHandler(message: String, t: Throwable) {
        DefaultLoader.getIdeHelper().invokeLater {
            if (isViewDetached) return@invokeLater
            mvpView.onErrorWithException(message, t as? Exception)
        }
    }
}