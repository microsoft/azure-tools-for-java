package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.List;

public class PricingTierComboBox extends AzureComboBox<PricingTier> {
    public static PricingTier DEFAULT_TIER = PricingTier.BASIC_B2;

    @NotNull
    @Override
    protected List<? extends PricingTier> loadItems() throws Exception {
        return AzureMvpModel.getInstance().listPricingTier();
    }
}
