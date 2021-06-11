/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;

@Setter
@Getter
public class DraftServicePlan extends AppServicePlanEntity implements Draft {
    @Getter
    @Setter
    private Subscription subscription;

    public DraftServicePlan(@Nonnull Subscription subscription,
                            @Nonnull String name,
                            @Nonnull Region region,
                            @Nonnull OperatingSystem os,
                            @Nonnull PricingTier pricingTier) {
        super(builder().subscriptionId(subscription.getId()).name(name).region(region.getName()).pricingTier(pricingTier).operatingSystem(os));
        this.subscription = subscription;
    }
}
