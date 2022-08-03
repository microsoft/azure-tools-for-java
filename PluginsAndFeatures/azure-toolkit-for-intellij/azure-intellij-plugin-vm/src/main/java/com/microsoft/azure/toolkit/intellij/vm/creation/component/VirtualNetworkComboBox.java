/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.Network;
import com.microsoft.azure.toolkit.lib.network.virtualnetwork.NetworkDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class VirtualNetworkComboBox extends AzureComboBox<Network> {
    private NetworkDraft draft;
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        resetToDraft();
        this.reloadItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        if (Objects.equals(resourceGroup, this.resourceGroup)) {
            return;
        }
        this.resourceGroup = resourceGroup;
        resetToDraft();
        this.reloadItems();
    }

    public void setRegion(Region region) {
        if (Objects.equals(region, this.region)) {
            return;
        }
        this.region = region;
        resetToDraft();
        this.reloadItems();
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create new virtual network (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::createVirtualNetwork);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void resetToDraft() {
        final Network value = getValue();
        if (value != null && Objects.nonNull(subscription) && !value.isDraftForCreating()) {
            final String name = NetworkDraft.generateDefaultName();
            final String rgName = Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse("<none>");
            this.draft = Azure.az(AzureNetwork.class).virtualNetworks(subscription.getId()).create(name, rgName);
            this.draft.withDefaultConfig();
            this.draft.setRegion(region);
            setValue(this.draft);
        }
    }

    private void createVirtualNetwork() {
        if (!ObjectUtils.allNotNull(resourceGroup, region, subscription)) {
            AzureMessager.getMessager().warning("To create new virtual network, please select subscription, resource group and region first");
            return;
        }
        final String name = NetworkDraft.generateDefaultName();
        final String rgName = Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse("<none>");
        final NetworkDraft defaultNetwork = Azure.az(AzureNetwork.class).virtualNetworks(subscription.getId()).create(name, rgName);
        defaultNetwork.withDefaultConfig();
        final VirtualNetworkDialog dialog = new VirtualNetworkDialog(subscription.getId(), resourceGroup.getName(), region);
        dialog.setValue(defaultNetwork);
        if (dialog.showAndGet()) {
            this.draft = dialog.getValue();
            this.addItem(draft);
            setValue(draft);
        }
    }

    public void setData(Network value) {
        this.draft = value.isDraftForCreating() ? (NetworkDraft) value : null;
        super.setValue(new ItemReference<>(resource -> StringUtils.equals(value.getName(), resource.getName()) &&
            StringUtils.equals(value.getResourceGroupName(), resource.getResourceGroupName())));
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Network) {
            final Network network = (Network) item;
            return (network.isDraftForCreating() ? "(New) " : "") + network.getName();
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<? extends Network> loadItems() throws Exception {
        final List<Network> networks = subscription == null ? Collections.emptyList() :
            Azure.az(AzureNetwork.class).virtualNetworks(subscription.getId())
                .list().stream().filter(network -> Objects.equals(network.getRegion(), region)).collect(Collectors.toList());
        return draft == null ? networks : ListUtils.union(List.of(draft), networks);
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureNetwork.class).virtualNetworks(s.getId()).refresh());
        super.refreshItems();
    }
}
