/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.storage.creation.VMStorageAccountCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import com.microsoft.azure.toolkit.lib.storage.service.AzureStorageAccount;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureStorageAccountComboBox extends AzureComboBox<StorageAccountConfig> {
    private static final StorageAccountConfig NONE = StorageAccountConfig.builder().name("None").build();
    private Subscription subscription;
    private StorageAccountConfig draft;

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
        this.refreshItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof StorageAccountConfig) {
            final String name = ((StorageAccountConfig) item).getName();
            return StringUtils.isEmpty(((StorageAccountConfig) item).getId()) && item != NONE ? "(New) " + name : name;
        }
        return super.getItemText(item);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(AllIcons.General.Add, "Create new storage account", this::createStorageAccount);
    }

    @Override
    @Nullable
    public StorageAccountConfig getValue() {
        final StorageAccountConfig result = super.getValue();
        return result == NONE ? null : result;
    }

    public void setData(StorageAccountConfig value) {
        if (value == null) {
            super.setValue(NONE);
            return;
        }
        if (StringUtils.isEmpty(value.getId())) {
            // draft resource
            draft = value;
        }
        setValue(new ItemReference<>(resource -> StringUtils.equals(value.getName(), resource.getName()) &&
                StringUtils.equals(value.getResourceGroup().getName(), resource.getResourceGroup().getName())));
    }

    private void createStorageAccount() {
        final VMStorageAccountCreationDialog creationDialog = new VMStorageAccountCreationDialog(null);
        if (creationDialog.showAndGet()) {
            draft = creationDialog.getValue();
            this.addItem(draft);
            setValue(draft);
        }
    }

    @Nonnull
    @Override
    protected List<? extends StorageAccountConfig> loadItems() {
        final List<StorageAccountConfig> resources = Optional.ofNullable(subscription)
                .map(subscription -> Azure.az(AzureStorageAccount.class).subscription(subscription.getId()).list().stream()
                        .map(account -> StorageAccountConfig.builder().id(account.id()).name(account.name())
                                .resourceGroup(ResourceGroup.builder().name(account.resourceGroup()).build()).subscription(account.subscription()).build()).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        if (draft != null) {
            // Clean draft reference if the resource has been created
            resources.stream().filter(storageAccount -> StringUtils.equals(storageAccount.getName(), draft.getName()) &&
                            StringUtils.equals(storageAccount.getResourceGroupName(), draft.getResourceGroupName()))
                    .findFirst()
                    .ifPresent(resource -> this.draft = null);
        }
        final List<StorageAccountConfig> additionalList = Stream.of(NONE, draft).distinct().filter(Objects::nonNull).collect(Collectors.toList());
        return ListUtils.union(additionalList, resources);
    }
}
