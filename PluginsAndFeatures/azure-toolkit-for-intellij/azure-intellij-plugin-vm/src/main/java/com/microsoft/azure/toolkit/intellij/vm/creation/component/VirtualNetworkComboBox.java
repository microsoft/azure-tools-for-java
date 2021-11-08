/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.DraftNetwork;
import com.microsoft.azure.toolkit.lib.compute.network.Network;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VirtualNetworkComboBox extends AzureComboBox<Network> {
    private DraftNetwork draftNetwork;
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setSubscriptionId(subscription.getId()));
        resetResourceDraft();
        this.refreshItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setResourceGroup(resourceGroup.getName()));
    }

    public void setRegion(Region region) {
        this.region = region;
        Optional.ofNullable(draftNetwork).ifPresent(draftNetwork -> draftNetwork.setRegion(region));
        resetResourceDraft();
        this.refreshItems();
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Create new virtual network", this::createVirtualNetwork);
    }

    private void resetResourceDraft() {
        final Network value = getValue();
        if (value != null && !StringUtils.equals(value.status(), IAzureBaseResource.Status.DRAFT)) {
            draftNetwork = DraftNetwork.getDefaultNetworkDraft();
            draftNetwork.setRegion(region);
            draftNetwork.setResourceGroup(Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse(null));
            draftNetwork.setSubscriptionId(Optional.ofNullable(subscription).map(Subscription::getId).orElse(null));
            setValue(draftNetwork);
        }
    }

    private void createVirtualNetwork() {
        if (!ObjectUtils.allNotNull(resourceGroup, region, subscription)) {
            AzureMessager.getMessager().warning("To create new virtual network, please select subscription, resource group and region first");
            return;
        }
        final DraftNetwork defaultNetwork = DraftNetwork.getDefaultNetworkDraft();
        final VirtualNetworkDialog dialog = new VirtualNetworkDialog(subscription.getId(), resourceGroup.getName(), region);
        dialog.setValue(defaultNetwork);
        if (dialog.showAndGet()) {
            this.draftNetwork = dialog.getValue();
            this.addItem(draftNetwork);
            setValue(draftNetwork);
        }
    }

    public void setData(Network value) {
        if (value instanceof DraftNetwork) {
            draftNetwork = StringUtils.equals(value.status(), IAzureBaseResource.Status.DRAFT) ? (DraftNetwork) value : null;
        }
        setValue(new ItemReference<>(resource -> StringUtils.equals(value.getName(), resource.getName()) &&
                StringUtils.equals(value.getResourceGroup(), resource.getResourceGroup())));
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Network) {
            return item instanceof AzureResourceDraft ? "(New) " + ((Network) item).name() : ((Network) item).name();
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Network> loadItems() throws Exception {
        final List<Network> networks = subscription == null ? Collections.emptyList() : Azure.az(AzureNetwork.class).subscription(subscription.getId())
                .list().stream().filter(network -> Objects.equals(network.getRegion(), region)).collect(Collectors.toList());
        return draftNetwork == null ? networks : ListUtils.union(List.of(draftNetwork), networks);
    }
}
