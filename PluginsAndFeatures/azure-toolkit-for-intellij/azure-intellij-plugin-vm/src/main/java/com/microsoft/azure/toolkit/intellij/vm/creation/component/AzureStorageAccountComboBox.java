/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.service.StorageAccount;
import org.apache.commons.collections4.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureStorageAccountComboBox extends AzureComboBox<StorageAccountConfig> {
    private Subscription subscription;
    private StorageAccountConfig draft;

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof StorageAccount ? ((StorageAccount) item).name() : super.getItemText(item);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Create new storage account", this::createStorageAccount);
    }

    private void createStorageAccount() {

    }

    @Nonnull
    @Override
    protected List<? extends StorageAccountConfig> loadItems() {
        final List<StorageAccountConfig> resources = Optional.ofNullable(subscription)
                .map(subscription -> Azure.az(AzureStorageAccount.class).subscription(subscription.getId()).list().stream()
                        .map(account -> StorageAccountConfig.builder().id(account.id()).name(account.name())
                                .resourceGroup(ResourceGroup.builder().name(account.resourceGroup()).build()).subscription(account.subscription()).build()).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        return draft == null ? resources : ListUtils.union(Arrays.asList(draft), resources);
    }
}
