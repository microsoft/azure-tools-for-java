/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;

public class AzResourceNode<T extends AzResource> extends Node<T> {
    private final Debouncer refreshViewLater = new TailingDebouncer(this::refreshViewLater, 300);
    private final AzureEventBus.EventListener listener;
    @Getter
    private View view;

    public AzResourceNode(@Nonnull T resource) {
        super(resource);
        this.withIcon(DEFAULT_AZURE_RESOURCE_ICON_PROVIDER::getIcon);
        this.withLabel(AzResource::getName);
        this.withDescription(AzResource::getStatus);
        this.enableWhen(r -> !r.getFormalStatus().isDeleted());

        this.listener = new AzureEventBus.EventListener(this::onEvent);
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("resource.status_changed.resource", listener);
        AzureEventBus.on("resource.children_changed.resource", listener);

        this.view = new View(AzureIcons.Common.REFRESH_ICON, this.buildLabel());
        this.onViewChanged();
    }

    public void onEvent(AzureEvent event) {
        final T data = this.getData();
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof AzResource &&
            StringUtils.equals(((AzResource) source).getId(), data.getId()) &&
            StringUtils.equals(((AzResource) source).getName(), data.getName())) {
            if (StringUtils.equals(type, "resource.refreshed.resource")) {
                this.onViewChanged();
                this.onChildrenChanged(false);
            } else if (StringUtils.equals(type, "resource.status_changed.resource")) {
                this.onViewChanged();
            } else if (StringUtils.equals(type, "resource.children_changed.resource")) {
                this.onChildrenChanged(true);
            }
        }
    }

    @Override
    public void onViewChanged() {
        this.refreshViewLater.debounce();
    }

    private void refreshViewLater() {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> {
            this.view = this.buildView();
            super.onViewChanged();
        });
    }

    public void dispose() {
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("resource.status_changed.resource", listener);
        AzureEventBus.off("resource.children_changed.resource", listener);
        this.setViewChangedListener(null);
        this.setChildrenChangedListener(null);
    }

    @Override
    public String buildDescription() {
        final boolean deleted = this.getData().getFormalStatus().isDeleted();
        return deleted ? AzResource.Status.DELETED : super.buildDescription();
    }
}