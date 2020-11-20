/**
 * Copyright (c) 2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.forms.appservice.base

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Signal
import com.microsoft.azure.arm.resources.Region
import com.microsoft.azure.management.appservice.AppServicePlan
import com.microsoft.azure.management.appservice.PricingTier
import com.microsoft.azure.management.resources.Location
import com.microsoft.azure.management.resources.ResourceGroup
import com.microsoft.azure.management.resources.Subscription
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel
import com.microsoft.intellij.helpers.base.AzureMvpPresenter
import org.jetbrains.plugins.azure.RiderAzureBundle.message

abstract class CreateAppViewPresenter<V : CreateAppMvpView> : AzureMvpPresenter<V>() {

    private val subscriptionSignal = Signal<List<Subscription>>()
    private val resourceGroupSignal = Signal<List<ResourceGroup>>()
    private val appServicePlanSignal = Signal<List<AppServicePlan>>()
    private val locationSignal = Signal<List<Location>>()
    private val pricingTierSignal = Signal<List<PricingTier>>()

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
                { AzureMvpModel.getInstance().listLocationsBySubscriptionId(subscriptionId)
                        // TODO: This is a workaround for locations that cause exceptions on create entries.
                        .filter { location -> Region.values().any { region -> region.name().equals(location.name(), true) } }
                },
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
}
