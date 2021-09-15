/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component.ip;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.compute.AzureResourceDraft;
import com.microsoft.azure.toolkit.lib.compute.ip.AzurePublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.ip.DraftPublicIpAddress;
import com.microsoft.azure.toolkit.lib.compute.ip.PublicIpAddress;
import lombok.Setter;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PublicIPAddressComboBox extends AzureComboBox<PublicIpAddress> {

    public static final PublicIpAddress NONE = new DraftPublicIpAddress("null", "null", "None");

    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private Region region;
    private DraftPublicIpAddress draftPublicIpAddress;

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof PublicIpAddress) {
            final String name = ((PublicIpAddress) item).name();
            return item instanceof AzureResourceDraft && item != NONE ? "(New) " + name : name;
        }
        return super.getItemText(item);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        Optional.ofNullable(draftPublicIpAddress).ifPresent(draftNetwork -> draftNetwork.setSubscriptionId(subscription.getId()));
        this.refreshItems();
    }

    public void setResourceGroup(ResourceGroup resourceGroup) {
        this.resourceGroup = resourceGroup;
        Optional.ofNullable(draftPublicIpAddress).ifPresent(draftNetwork -> draftNetwork.setResourceGroup(resourceGroup.getName()));
    }

    public void setRegion(Region region) {
        this.region = region;
        Optional.ofNullable(draftPublicIpAddress).ifPresent(draftNetwork -> draftNetwork.setRegion(region));
    }

    @Override
    @Nullable
    public PublicIpAddress getValue() {
        final PublicIpAddress result = super.getValue();
        return result == NONE ? null : result;
    }

    @Override
    public void setValue(PublicIpAddress address) {
        if (address == null) {
            super.setValue(NONE);
            return;
        }
        if (address instanceof DraftPublicIpAddress && StringUtils.equals(address.status(), IAzureBaseResource.Status.DRAFT)) {
            draftPublicIpAddress = (DraftPublicIpAddress) address;
        }
        super.setValue(address);
    }

    @Nonnull
    @Override
    protected List<? extends PublicIpAddress> loadItems() {
        if (Objects.isNull(subscription)) {
            return Collections.emptyList();
        }
        final List<PublicIpAddress> list = Azure.az(AzurePublicIpAddress.class).subscription(subscription.getId()).list();
        final List<PublicIpAddress> additionalList = Stream.of(NONE, draftPublicIpAddress).filter(Objects::nonNull).collect(Collectors.toList());
        return ListUtils.union(additionalList, list);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, AzureMessageBundle.message("common.resourceGroup.create.tooltip").toString(), this::showPublicIpCreationPopup);
    }

    private void showPublicIpCreationPopup() {
        if (!ObjectUtils.allNotNull(resourceGroup, region, subscription)) {
            AzureMessager.getMessager().warning("To create new public ip address, please select subscription, resource group and region first");
            return;
        }
        final PublicIpAddressCreationDialog dialog = new PublicIpAddressCreationDialog(this.subscription, this.resourceGroup, this.region);
        if (dialog.showAndGet()) {
            this.draftPublicIpAddress = dialog.getData();
            this.addItem(draftPublicIpAddress);
            setValue(draftPublicIpAddress);
        }
    }
}
