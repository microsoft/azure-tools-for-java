/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.network.networksecuritygroup.NetworkSecurityGroup;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SecurityGroupComboBox extends AzureComboBox<NetworkSecurityGroup> {
    private Region region;
    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        this.reloadItems();
    }

    public void setRegion(Region region) {
        this.region = region;
        this.reloadItems();
    }

    @Nullable
    @Override
    protected NetworkSecurityGroup doGetDefaultValue() {
        return CacheManager.getUsageHistory(NetworkSecurityGroup.class)
            .peek(v -> (Objects.isNull(subscription) || Objects.equals(subscription, v.getSubscription())) &&
                (Objects.isNull(region) || Objects.equals(region, v.getRegion())));
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof NetworkSecurityGroup ? ((NetworkSecurityGroup) item).name() : super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends NetworkSecurityGroup> loadItems() throws Exception {
        if (subscription == null) {
            return Collections.emptyList();
        } else {
            final AzureNetwork az = Azure.az(AzureNetwork.class);
            return az.networkSecurityGroups(subscription.getId()).list().stream()
                .filter(group -> Objects.equals(group.getRegion(), region)).collect(Collectors.toList());
        }
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(subscription).ifPresent(s -> Azure.az(AzureNetwork.class).networkSecurityGroups(s.getId()).refresh());
        super.refreshItems();
    }

    public void setData(NetworkSecurityGroup networkSecurityGroup) {
        setValue(new ItemReference<>(resource -> StringUtils.equals(resource.getName(), networkSecurityGroup.getName()) &&
            StringUtils.equals(resource.getResourceGroupName(), networkSecurityGroup.getResourceGroupName())));
    }
}
