/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.subscription;

import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;

import java.util.List;
import java.util.Objects;

public class SubscriptionComboBox extends AzureComboBox<Subscription> {

    public SubscriptionComboBox() {
        super();
        final List<Subscription> items = AzureMvpModel.getInstance().getSelectedSubscriptions();
        this.setValue(items.get(0)); // select the first subscription
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "account|subscription.list.selected",
        type = AzureOperation.Type.SERVICE
    )
    protected List<Subscription> loadItems() throws Exception {
        return AzureMvpModel.getInstance().getSelectedSubscriptions();
    }

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        return ((Subscription) item).displayName();
    }
}
