/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class PricingTierComboBox extends AzureComboBox<PricingTier> {

    private List<? extends PricingTier> pricingTierList = Collections.emptyList();

    public PricingTierComboBox() {
        super();
    }

    public void setDefaultPricingTier(final PricingTier defaultPricingTier) {
        setValue(defaultPricingTier);
    }

    public void setPricingTierList(final List<? extends PricingTier> pricingTierList) {
        this.pricingTierList = pricingTierList;
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final PricingTier pricingTier = (PricingTier) item;
        return Objects.equals(pricingTier, PricingTier.CONSUMPTION) ?
            message("appService.pricingTier.consumption") : pricingTier.getTier() + "_" + pricingTier.getSize();
    }

    @Nonnull
    @Override
    protected List<? extends PricingTier> loadItems() throws Exception {
        return pricingTierList;
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
