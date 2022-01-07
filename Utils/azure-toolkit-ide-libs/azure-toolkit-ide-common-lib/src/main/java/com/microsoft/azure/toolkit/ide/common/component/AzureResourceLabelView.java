/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureResourceLabelView<T extends IAzureBaseResource<?, ?>> implements NodeView {
    @Nonnull
    @Getter
    private final T resource;
    @Getter
    private final String label;
    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    @Getter
    private String description;
    @Nullable
    @Setter
    @Getter
    private Refresher refresher;

    public AzureResourceLabelView(@Nonnull T resource) {
        this.resource = resource;
        this.label = resource.name();
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("resource.refresh.resource", listener);
        AzureEventBus.on("common|resource.status_changed", listener);
        AzureEventBus.on("resource.children_changed.resource", listener);
        AzureEventBus.on("resource.status_changed.resource", listener);
        AzureEventBus.on("module.children_changed.module", listener);
        this.refreshView();
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof IAzureBaseResource && ((IAzureBaseResource<?, ?>) source).id().equals(this.resource.id())) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            switch (type) {
                case "resource.refresh.resource":
                    if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                        tm.runLater(this::refreshChildren);
                    }
                    break;
                case "common|resource.status_changed":
                case "resource.status_changed.resource":
                    tm.runLater(this::refreshView);
                    break;
                case "resource.children_changed.resource":
                case "module.children_changed.module":
                    tm.runLater(this::refreshChildren);
                    break;
            }
        }
    }

    public void dispose() {
        AzureEventBus.off("resource.refresh.resource", listener);
        AzureEventBus.off("common|resource.status_changed", listener);
        AzureEventBus.off("resource.children_changed.resource", listener);
        AzureEventBus.off("resource.status_changed.resource", listener);
        AzureEventBus.off("module.children_changed.module", listener);
        this.refresher = null;
    }

    public String getIconPath() {
        final String formalStatus = resource.getFormalStatus();
        if (resource instanceof AzResource) {
            return getIconPath((AzResource<?, ?, ?>) resource);
        }
        final String type = resource.getClass().getSimpleName().toLowerCase();
        final String icon = StringUtils.isBlank(formalStatus) ? type : String.format("%s-%s", type, formalStatus.toLowerCase().trim());
        return String.format("/icons/%s.svg", icon);
    }

    @Nonnull
    private String getIconPath(AzResource<?, ?, ?> resource) {
        final String status = resource.getStatus();
        AzResource<?, ?, ?> current = resource;
        final StringBuilder modulePath = new StringBuilder();
        while (!(current instanceof AzResource.None)) {
            modulePath.insert(0, "/" + current.getModule().getName());
            current = current.getParent();
        }
        String fallback = String.format("/icons%s/default.svg", modulePath);
        if (status.toLowerCase().endsWith("ing")) {
            fallback = "/icons/spinner";
        } else if (status.toLowerCase().endsWith("ed")) {
            fallback = "/icons/error";
        }
        final String iconPath = String.format("/icons%s/%s.svg", modulePath, status.toLowerCase());
        return iconPath + ":" + fallback;
    }
}
