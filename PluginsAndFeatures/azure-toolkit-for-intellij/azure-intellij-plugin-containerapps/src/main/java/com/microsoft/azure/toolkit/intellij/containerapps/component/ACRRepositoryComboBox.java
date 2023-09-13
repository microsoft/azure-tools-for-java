/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Repository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ACRRepositoryComboBox extends AzureComboBox<Repository> {
    private ContainerRegistry registry;

    @Override
    public String getLabel() {
        return "Repository";
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Repository) item).getName();
    }

    public void setRegistry(ContainerRegistry registry) {
        if (Objects.equals(registry, this.registry)) {
            return;
        }
        this.registry = registry;
        if (registry == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Nullable
    @Override
    protected Repository doGetDefaultValue() {
        return CacheManager.getUsageHistory(Repository.class).peek(repo -> Objects.isNull(registry) || Objects.equals(registry.getId(), repo.getParent().getId()));
    }

    @Nonnull
    @Override
    protected List<? extends Repository> loadItems() {
        if (Objects.nonNull(this.registry)) {
            return this.registry.getRepositoryModule().list().stream()
                .sorted(Comparator.comparing(Repository::getName)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.registry).ifPresent(s -> this.registry.getRepositoryModule().refresh());
        super.refreshItems();
    }
}
