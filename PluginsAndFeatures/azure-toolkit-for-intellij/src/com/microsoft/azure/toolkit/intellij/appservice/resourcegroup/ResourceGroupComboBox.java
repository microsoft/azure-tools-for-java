/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.Mock;
import com.microsoft.azure.toolkit.lib.appservice.ResourceGroupMock;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private List<ResourceGroupMock> localItems = new ArrayList<>();

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        if (item instanceof Mock) {
            return "(New) " + ((ResourceGroup) item).name();
        }
        return ((ResourceGroup) item).name();
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
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
            AllIcons.General.Add, "Create new resource group", this::showResourceGroupCreationPopup);
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
