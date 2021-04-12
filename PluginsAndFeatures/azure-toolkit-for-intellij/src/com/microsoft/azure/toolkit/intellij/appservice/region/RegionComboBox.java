/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.region;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RegionComboBox extends AzureComboBox<Region> {

    private Subscription subscription;
    private PricingTier tier = PricingTier.BASIC_B2;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((Region) item).label();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    public void setPricingTier(PricingTier tier) {
        if (Objects.equals(tier, this.tier)) {
            return;
        }
        this.tier = tier;
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "appservice|region.list.subscription|tier",
        params = {"@tier.toString()", "@subscription.subscriptionId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Region> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.subscriptionId();
            return AzureWebAppMvpModel.getInstance().getAvailableRegions(sid, tier);
        }
        return Collections.emptyList();
    }
}
