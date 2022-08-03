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
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.network.AzureNetwork;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddress;
import com.microsoft.azure.toolkit.lib.network.publicipaddress.PublicIpAddressDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PublicIPAddressComboBox extends AzureComboBox<PublicIpAddress> {
    private final PublicIpAddressDraft NONE;
    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;
    private PublicIpAddressDraft draft;

    public PublicIPAddressComboBox() {
        super();
        final Subscription subs = Azure.az(AzureAccount.class).account().getSelectedSubscriptions().get(0);
        NONE = Azure.az(AzureNetwork.class).publicIpAddresses(subs.getId()).create("<none>", "<none>");
    }

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

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof PublicIpAddress) {
            final PublicIpAddress ipAddress = (PublicIpAddress) item;
            final String name = ipAddress.getName();
            return ipAddress.isDraftForCreating() && ipAddress != NONE ? "(New) " + name : name;
        }
        return super.getItemText(item);
    }

    @Override
    @Nullable
    public PublicIpAddress getValue() {
        final PublicIpAddress result = super.getValue();
        return result == NONE ? null : result;
    }

    public void setData(PublicIpAddress address) {
        if (address == null) {
            super.setValue(NONE);
            return;
        }
        this.draft = address.isDraftForCreating() ? (PublicIpAddressDraft) address : null;
        super.setValue(new ItemReference<>(resource -> StringUtils.equals(address.getName(), resource.getName()) &&
            StringUtils.equals(address.getResourceGroupName(), resource.getResourceGroupName())));
    }

    @Nonnull
    @Override
    protected List<? extends PublicIpAddress> loadItems() {
        final List<PublicIpAddress> list = subscription == null ? Collections.emptyList() : Azure.az(AzureNetwork.class)
            .publicIpAddresses(subscription.getId()).list().stream()
            .filter(ip -> Objects.equals(ip.getRegion(), region) && !ip.hasAssignedNetworkInterface())
            .collect(Collectors.toList());
        final List<PublicIpAddress> additionalList = Stream.of(NONE, draft).distinct().filter(Objects::nonNull).collect(Collectors.toList());
        return ListUtils.union(additionalList, list);
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", AzureMessageBundle.message("common.resourceGroup.create.tooltip").toString(), KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::showPublicIpCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void resetToDraft() {
        final PublicIpAddress value = getValue();
        if (value != null && Objects.nonNull(subscription) && !value.isDraftForCreating()) {
            final String name = PublicIpAddressDraft.generateDefaultName();
            final String rgName = Optional.ofNullable(resourceGroup).map(ResourceGroup::getName).orElse("<none>");
            this.draft = Azure.az(AzureNetwork.class).publicIpAddresses(subscription.getId()).create(name, rgName);
            this.draft.setRegion(region);
            setValue(this.draft);
        }
    }

    private void showPublicIpCreationPopup() {
        if (!ObjectUtils.allNotNull(resourceGroup, region, subscription)) {
            AzureMessager.getMessager().warning("To create new public ip address, please select subscription, resource group and region first");
            return;
        }
        final PublicIpAddressCreationDialog dialog = new PublicIpAddressCreationDialog(this.subscription, this.resourceGroup, this.region);
        if (dialog.showAndGet()) {
            this.draft = dialog.getValue();
            this.addItem(draft);
            setValue(draft);
        }
    }
}
