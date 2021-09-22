/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.vm.AzureVirtualMachineSize;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VirtualMachineSizeComboBox extends AzureComboBox<AzureVirtualMachineSize> {
    private Region region;
    private Subscription subscription;

    public void setRegion(Region region) {
        this.region = region;
        this.refreshItems();
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof AzureVirtualMachineSize ? ((AzureVirtualMachineSize) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends AzureVirtualMachineSize> loadItems() throws Exception {
        if (Objects.isNull(region) || Objects.isNull(subscription)) {
            return Collections.emptyList();
        }
        return Azure.az(AzureVirtualMachine.class).listPricing(subscription.getId(), region);
    }
}
