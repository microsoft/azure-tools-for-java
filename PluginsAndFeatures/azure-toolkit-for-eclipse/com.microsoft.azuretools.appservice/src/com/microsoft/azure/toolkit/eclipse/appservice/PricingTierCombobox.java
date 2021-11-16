/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PricingTierCombobox extends AzureComboBox<PricingTier> {

    public PricingTierCombobox(@Nonnull Composite parent) {
        this(parent, null);
    }

    public PricingTierCombobox(@Nonnull Composite parent, List<PricingTier> pricingTiers) {
        super(parent, () -> Optional.ofNullable(pricingTiers).orElse(Collections.emptyList()));
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final PricingTier pricingTier = (PricingTier) item;
        return Objects.equals(pricingTier, PricingTier.CONSUMPTION) ?
                "Consumption" : pricingTier.getTier() + "_" + pricingTier.getSize();
    }

}
