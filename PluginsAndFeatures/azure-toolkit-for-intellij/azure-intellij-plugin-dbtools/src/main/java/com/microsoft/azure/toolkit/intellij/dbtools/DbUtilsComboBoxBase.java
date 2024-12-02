/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.dbtools;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public abstract class DbUtilsComboBoxBase<T extends AzResource> extends AzureComboBox<T> {
    private boolean noResources;
    private Consumer<Boolean> resourcesLoadedListener;

    public void setResourcesLoadedListener(@Nonnull Consumer<Boolean> serverListener) {
        this.resourcesLoadedListener = serverListener;
        serverListener.accept(this.noResources);
    }

    public void setByResourceId(@Nonnull String resourceId) {
        setValue(new AzureComboBox.ItemReference<>(i -> i.getId().equals(resourceId)));
    }

    public void setNull() {
        setValue((T)null);
    }

    @Nonnull
    @Override
    protected List<T> loadItems() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return Collections.emptyList();
        }

        final List<T> resources = load();
        this.noResources = CollectionUtils.isEmpty(resources);
        Optional.ofNullable(this.resourcesLoadedListener).ifPresent(l -> l.accept(this.noResources));
        return resources;
    }

    protected abstract List<T> load();

    @Override
    protected String getItemText(Object item) {
        return Optional.ofNullable(item)
                .filter(AzResource.class::isInstance)
                .map(AzResource.class::cast)
                .map(AzResource::getName)
                .orElse("");
    }
}
