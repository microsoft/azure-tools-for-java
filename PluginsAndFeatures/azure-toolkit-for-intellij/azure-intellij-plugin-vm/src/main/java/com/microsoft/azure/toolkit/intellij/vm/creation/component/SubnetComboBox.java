/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import com.microsoft.azure.toolkit.lib.compute.network.model.Subnet;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SubnetComboBox extends AzureComboBox<Subnet> {
    private Network network;

    public void setNetwork(Network network) {
        this.network = network;
        if (network instanceof AzureResourceDraft) {
            this.setValue(network.subnets().get(0));
        }
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Subnet ? ((Subnet) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Subnet> loadItems() throws Exception {
        return Optional.ofNullable(network).map(Network::subnets).orElse(Collections.emptyList());
    }
}
