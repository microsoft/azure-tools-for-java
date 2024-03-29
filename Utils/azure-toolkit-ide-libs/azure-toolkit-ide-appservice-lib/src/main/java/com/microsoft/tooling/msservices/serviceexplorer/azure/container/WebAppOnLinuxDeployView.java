/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.container;

import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;

import java.util.List;

public interface WebAppOnLinuxDeployView extends MvpView {
    void renderWebAppOnLinuxList(List<WebApp> webAppOnLinuxList);

    void renderSubscriptionList(List<Subscription> subscriptions);

    void renderResourceGroupList(List<ResourceGroup> resourceGroupList);

    void renderLocationList(List<Region> locationList);

    void renderPricingTierList(List<PricingTier> pricingTierList);

    void renderAppServicePlanList(List<AppServicePlan> appServicePlans);
}
