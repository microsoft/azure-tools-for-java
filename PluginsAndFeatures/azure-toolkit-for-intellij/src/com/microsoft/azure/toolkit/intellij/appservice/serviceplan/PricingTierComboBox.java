/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;

import java.util.Collections;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class PricingTierComboBox extends AzureComboBox<PricingTier> {

    private List<PricingTier> pricingTierList = Collections.EMPTY_LIST;

    public PricingTierComboBox() {
        super();
    }

    public void setDefaultPricingTier(final PricingTier defaultPricingTier) {
        setValue(defaultPricingTier);
    }

    public void setPricingTierList(final List<PricingTier> pricingTierList) {
        this.pricingTierList = pricingTierList;
    }

    @Override
    protected String getItemText(final Object item) {
        return item == AzureFunctionMvpModel.CONSUMPTION_PRICING_TIER ? message("appService.pricingTier.consumption") : super.getItemText(item);
    }

    @NotNull
    @Override
    protected List<? extends PricingTier> loadItems() throws Exception {
        return pricingTierList;
    }
}
