package com.microsoft.azure.appservice.component.form.input;

import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ComboBoxRegion extends AzureComboBox<Region> {

    private Subscription subscription;
    private PricingTier tier = PricingTier.BASIC_B2;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((Region) item).name();
    }

    public void refreshWith(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    public void refreshWith(Subscription subscription, PricingTier tier) {
        this.subscription = subscription;
        this.tier = tier;
        this.refreshItems();
    }

    @NotNull
    @Override
    protected List<? extends Region> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.subscriptionId();
            return AzureWebAppMvpModel.getInstance().getAvailableRegions(sid, tier);
        }
        return Collections.emptyList();
    }
}
