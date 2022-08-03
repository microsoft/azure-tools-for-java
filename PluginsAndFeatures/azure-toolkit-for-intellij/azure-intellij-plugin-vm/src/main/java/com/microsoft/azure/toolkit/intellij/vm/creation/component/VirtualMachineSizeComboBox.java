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
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VmSize;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class VirtualMachineSizeComboBox extends AzureComboBox<VmSize> {
    private Region region;
    private Subscription subscription;

    public void setRegion(Region region) {
        this.region = region;
        this.reloadItems();
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof VmSize ? ((VmSize) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends VmSize> loadItems() throws Exception {
        if (Objects.isNull(region) || Objects.isNull(subscription)) {
            return Collections.emptyList();
        }
        return Azure.az(AzureCompute.class).listSizes(subscription.getId(), region);
    }
}
