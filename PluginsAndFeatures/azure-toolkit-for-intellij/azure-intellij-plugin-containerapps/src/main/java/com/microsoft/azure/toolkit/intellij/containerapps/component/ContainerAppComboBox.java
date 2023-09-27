/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerAppComboBox extends AzureComboBox<ContainerApp> {
    private Subscription subscription;
    private final List<ContainerApp> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Registry";
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        final ContainerApp entity = (ContainerApp) item;
        if (entity.isDraftForCreating()) {
            return "(New) " + entity.getName();
        }
        return entity.getName();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable ContainerApp val, Boolean fixed) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nullable
    @Override
    protected ContainerApp doGetDefaultValue() {
        return CacheManager.getUsageHistory(ContainerApp.class)
            .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    protected List<? extends ContainerApp> loadItems() {
        Stream<AzureContainerAppsServiceSubscription> stream = Azure.az(AzureContainerApps.class).list().stream();
        if (Objects.nonNull(this.subscription)) {
            stream = stream.filter(s -> s.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
        }
        final List<ContainerApp> remoteApps = stream.flatMap(s -> s.containerApps().list().stream())
            .sorted(Comparator.comparing(ContainerApp::getName)).collect(Collectors.toList());
        final List<ContainerApp> apps = new ArrayList<>(remoteApps);
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            this.draftItems.stream()
                .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                .filter(i -> !remoteApps.contains(i)) // filter out the draft item which has been created
                .forEach(apps::add);
        }
        return apps;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureContainerApps.class).containerApps(s.getId()).refresh());
        super.refreshItems();
    }
}
