/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;

import java.util.Objects;

public class RegionComboBox extends AzureComboBox<Region> {

    @Getter
    private Subscription subscription;

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

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Region) {
            return ((Region) item).getLabel();
        }
        return super.getItemText(item);
    }
}
