/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.favorite;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public class FavoriteNodeView implements NodeView {

    @Nonnull
    private final AzureResourceLabelView<?> view;

    @Override
    public String getTips() {
        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            return "";
        }
        final AbstractAzResource<?, ?, ?> resource = (AbstractAzResource<?, ?, ?>) view.getResource();
        final ResourceId id = ResourceId.fromString(resource.getId());
        final Subscription subs = Azure.az(AzureAccount.class).account().getSubscription(id.subscriptionId());
        final String rg = id.resourceGroupName();
        final String typeName = resource.getModule().getResourceTypeName();
        return String.format("Type:%s | Subscription: %s | Resource Group:%s", typeName, subs.getName(), rg) +
            Optional.ofNullable(id.parent()).map(p -> " | Parent:" + p.name()).orElse("");
    }

    @Override
    public String getDescription() {
        final AzResource<?, ?, ?> r = view.getResource();
        if (r instanceof AbstractAzResource &&
            ((AbstractAzResource<?, ?, ?>) r).isDraftForCreating() &&
            !Objects.equals(r.getStatus(), AzResource.Status.CREATING)) {
            return AzResource.Status.DELETED;
        }
        return view.getDescription();
    }

    @Override
    public void refresh() {
        view.refresh();
    }

    @Override
    public void refreshView() {
        view.refreshView();
    }

    @Override
    public void refreshChildren(boolean... incremental) {
        view.refreshChildren(incremental);
    }

    @Override
    public AzureIcon getIcon() {
        return view.getIcon();
    }

    @Override
    public void setRefresher(Refresher refresher) {
        view.setRefresher(refresher);
    }

    @Override
    @Nullable
    public Refresher getRefresher() {
        return view.getRefresher();
    }

    @Override
    public String getLabel() {
        return view.getLabel();
    }

    @Override
    public String getIconPath() {
        return view.getIconPath();
    }

    @Override
    public boolean isEnabled() {
        final AzResource<?, ?, ?> r = view.getResource();
        if (r instanceof AbstractAzResource &&
            ((AbstractAzResource<?, ?, ?>) r).isDraftForCreating() &&
            !Objects.equals(r.getStatus(), AzResource.Status.CREATING)) {
            return false;
        }
        return view.isEnabled();
    }

    @Override
    public void dispose() {
        view.dispose();
    }
}
