/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.storage.creation.VMStorageAccountCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.model.StorageAccountConfig;
import org.apache.commons.collections4.ListUtils;
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

public class AzureStorageAccountComboBox extends AzureComboBox<StorageAccountConfig> {
    private static final StorageAccountConfig NONE = StorageAccountConfig.builder().name("None").build();
    private Subscription subscription;
    private StorageAccountConfig draft;

    public void setSubscription(final Subscription subscription) {
        this.subscription = subscription;
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof StorageAccountConfig) {
            final String name = ((StorageAccountConfig) item).getName();
            return StringUtils.isEmpty(((StorageAccountConfig) item).getId()) && item != NONE ? "(New) " + name : name;
        }
        return super.getItemText(item);
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("Create new storage account (%s)", KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::createStorageAccount);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
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

    @Override
    protected void refreshItems() {
        Optional.ofNullable(subscription).ifPresent(s -> Azure.az(AzureStorageAccount.class).accounts(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<? extends StorageAccountConfig> loadItems() {
        final List<StorageAccountConfig> resources = Optional.ofNullable(subscription)
            .map(subscription -> Azure.az(AzureStorageAccount.class).accounts(subscription.getId()).list().stream()
                .map(account -> StorageAccountConfig.builder().id(account.getId()).name(account.getName())
                    .resourceGroup(account.getResourceGroup())
                    .subscription(account.getSubscription()).build()).collect(Collectors.toList()))
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
