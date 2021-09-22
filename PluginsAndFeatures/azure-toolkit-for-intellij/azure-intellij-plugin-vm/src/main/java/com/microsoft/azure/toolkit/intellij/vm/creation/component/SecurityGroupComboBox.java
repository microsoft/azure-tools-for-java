/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.security.AzureNetworkSecurityGroup;
import com.microsoft.azure.toolkit.lib.compute.security.NetworkSecurityGroup;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SecurityGroupComboBox extends AzureComboBox<NetworkSecurityGroup> {
    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof NetworkSecurityGroup ? ((NetworkSecurityGroup) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends NetworkSecurityGroup> loadItems() throws Exception {
        return Optional.ofNullable(subscription).map(subscription -> Azure.az(AzureNetworkSecurityGroup.class).list(subscription.getId())).orElse(Collections.emptyList());
    }
}
