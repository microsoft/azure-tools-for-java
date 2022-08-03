/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class RegionComboBox extends AzureComboBox<Region> {

    protected Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((Region) item).getLabel();
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
        this.reloadItems();
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "appservice.list_regions.tier|subscription",
        params = {"this.tier.toString()", "this.subscription.getId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends Region> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            return az(AzureAccount.class).listRegions(sid);
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }
}
