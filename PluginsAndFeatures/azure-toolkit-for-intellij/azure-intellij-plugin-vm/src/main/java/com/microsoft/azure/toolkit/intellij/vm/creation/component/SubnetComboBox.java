/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.Network;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.Subnet;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SubnetComboBox extends AzureComboBox<Subnet> {
    private Network network;

    public void setNetwork(Network network) {
        this.network = network;
        if (network instanceof AzResource.Draft) {
            this.setValue(network.getSubnets().get(0));
        }
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof Subnet ? ((Subnet) item).getName() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Subnet> loadItems() throws Exception {
        return Optional.ofNullable(network).map(Network::getSubnets).orElse(Collections.emptyList());
    }
}
