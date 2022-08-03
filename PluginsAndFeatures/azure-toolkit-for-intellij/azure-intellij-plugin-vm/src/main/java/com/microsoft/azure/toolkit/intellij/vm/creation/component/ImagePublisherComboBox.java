/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmImagePublisher;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ImagePublisherComboBox extends AzureComboBox<VmImagePublisher> {
    private Subscription subscription;
    private Region region;

    public ImagePublisherComboBox(Subscription subscription, Region region) {
        super(false);
        this.subscription = subscription;
        this.region = region;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.clear();
        this.reloadItems();
    }

    public void setRegion(Region region) {
        this.region = region;
        this.clear();
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VmImagePublisher ? ((VmImagePublisher) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends VmImagePublisher> loadItems() throws Exception {
        if (Objects.isNull(subscription) || Objects.isNull(region)) {
            return Collections.emptyList();
        }
        return Azure.az(AzureCompute.class).listPublishers(subscription.getId(), region);
    }
}
