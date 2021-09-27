/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class SupportedRegionComboBox extends RegionComboBox {
    private final AzureService service;

    protected List<? extends Region> loadItems() {
        if (Objects.nonNull(this.subscription)) {
            return loadSupportedRegions(service, subscription.getId());
        }
        return Collections.emptyList();
    }

    public static List<? extends Region> loadSupportedRegions(AzureService service, String subscriptionId) {
        // this method is a legacy method since service.listSupportedRegions will lost the sequence
        // this method will be deleted onece the sequence in service.listSupportedRegions is fixed.
        final List<Region> regions = Azure.az(AzureAccount.class).listRegions(subscriptionId);
        final List supportedRegions = service.listSupportedRegions(subscriptionId);
        return regions.stream().filter(supportedRegions::contains).collect(Collectors.toList());
    }
}
