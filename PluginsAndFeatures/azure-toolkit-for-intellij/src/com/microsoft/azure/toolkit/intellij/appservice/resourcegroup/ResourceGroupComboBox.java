/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.Draft;
import com.microsoft.azure.toolkit.lib.appservice.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private final List<DraftResourceGroup> localItems = new ArrayList<>();

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        if (item instanceof Draft) {
            return "(New) " + ((ResourceGroup) item).name();
        }
        return ((ResourceGroup) item).name();
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
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "arm|rg.list.subscription",
        params = {"@subscription.subscriptionId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ResourceGroup> loadItems() throws Exception {
        final List<ResourceGroup> groups = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.localItems)) {
                groups.addAll(this.localItems.stream()
                    .filter(i -> this.subscription.equals(i.getSubscription()))
                    .collect(Collectors.toList()));
            }
            final String sid = subscription.subscriptionId();
            final List<ResourceGroup> remoteGroups = AzureMvpModel
                .getInstance()
                .getResourceGroupsBySubscriptionId(sid);
            groups.addAll(remoteGroups);
        }
        return groups;
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
            AllIcons.General.Add, message("appService.resourceGroup.create.tooltip"), this::showResourceGroupCreationPopup);
    }

    private void showResourceGroupCreationPopup() {
        final ResourceGroupCreationDialog dialog = new ResourceGroupCreationDialog(this.subscription);
        dialog.setOkActionListener((group) -> {
            this.localItems.add(0, group);
            dialog.close();
            final List<ResourceGroup> items = this.getItems();
            items.add(0, group);
            this.setItems(items);
            this.setValue(group);
        });
        dialog.show();
    }
}
