/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.graph.models.Application;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Combobox listing Azure AD applications.
 */
class AzureApplicationComboBox extends AzureComboBox<Application> {
    @Nullable
    private Subscription subscription;
    @Nullable
    private List<Application> predefinedItems;

    AzureApplicationComboBox() {
        super(false);

        this.setEditable(false);
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Application) {
            return ((Application) item).displayName;
        }
        return super.getItemText(item);
    }

    public void setPredefinedItems(@Nonnull List<Application> items) {
        this.predefinedItems = items;
        this.subscription = null;
        clear();
        reloadItems();
    }

    public void setSubscription(@Nullable Subscription subscription) {
        this.predefinedItems = null;
        this.subscription = subscription;
        clear();
        reloadItems();
    }

    @NotNull
    @Override
    protected List<? extends Application> loadItems() throws Exception {
        var items = predefinedItems;
        if (items != null && !items.isEmpty()) {
            return items;
        }

        var subscription = this.subscription;
        if (subscription != null) {
            var graphClient = AzureUtils.createGraphClient(subscription);
            return AzureUtils.loadApplications(graphClient)
                    .stream()
                    .sorted(Comparator.comparing(a -> StringUtil.defaultIfEmpty(a.displayName, "")))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
