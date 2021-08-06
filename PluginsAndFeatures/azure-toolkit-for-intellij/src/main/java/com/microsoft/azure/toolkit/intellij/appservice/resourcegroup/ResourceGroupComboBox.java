/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.Draft;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private final List<ResourceGroup> draftItems = new ArrayList<>();

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        ResourceGroup entity = (ResourceGroup) item;
        if (item instanceof Draft) {
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
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "arm|rg.list.subscription",
        params = {"this.subscription.getId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ResourceGroup> loadItems() throws Exception {
        final List<ResourceGroup> groups = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                groups.addAll(this.draftItems.stream()
                    .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                    .collect(Collectors.toList()));
            }
            final String sid = subscription.getId();
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
            this.draftItems.add(0, group);
            dialog.close();
            final List<ResourceGroup> items = new ArrayList<>(this.getItems());
            items.add(0, group);
            this.setItems(items);
            this.setValue(group);
        });
        dialog.show();
    }
}
