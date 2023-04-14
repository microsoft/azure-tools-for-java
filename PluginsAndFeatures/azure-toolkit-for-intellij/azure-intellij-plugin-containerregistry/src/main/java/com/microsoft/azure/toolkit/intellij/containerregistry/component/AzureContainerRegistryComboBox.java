/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;


import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AzureContainerRegistryComboBox extends AzureComboBox<ContainerRegistry> {
    private boolean listAllSubscription;
    private Subscription subscription;

    public AzureContainerRegistryComboBox(boolean listAllSubscription) {
        super(false);
        this.listAllSubscription = listAllSubscription;
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null && !listAllSubscription) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    protected String getItemText(Object item) {
        return item instanceof ContainerRegistry ? ((ContainerRegistry) item).getName() : super.getItemText(item);
    }

    @Nullable
    @Override
    protected Icon getItemIcon(Object item) {
        return item instanceof ContainerRegistry ? IntelliJAzureIcons.getIcon(AzureIcons.ContainerRegistry.MODULE) : super.getItemIcon(item);
    }

    @Nonnull
    @Override
    protected List<? extends ContainerRegistry> loadItems() throws Exception {
        final Stream<Subscription> subscriptionStream = Optional.ofNullable(subscription).map(Stream::of).orElseGet(()-> !listAllSubscription ?  Stream.empty() :
                Azure.az(AzureAccount.class).account().getSelectedSubscriptions().stream());
        return subscriptionStream.map(s -> Azure.az(AzureContainerRegistry.class).registry(s.getId()))
                .flatMap(module -> module.list().stream()).collect(Collectors.toList());
    }
}
