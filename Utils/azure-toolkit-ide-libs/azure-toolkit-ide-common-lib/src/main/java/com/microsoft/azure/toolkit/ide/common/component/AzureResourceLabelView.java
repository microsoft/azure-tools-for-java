/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureResourceLabelView<T extends IAzureResource<?>> implements IView.Label, IView.Dynamic {
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
    private Updater updater;

    public AzureResourceLabelView(@Nonnull T resource) {
        this.resource = resource;
        this.label = resource.name();
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("common|resource.refresh", listener);
        AzureEventBus.on("common|resource.status_changed", listener);
        this.updateView();
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof IAzureResource && ((IAzureResource<?>) source).id().equals(this.resource.id())) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            if ("common|resource.refresh".equals(type)) {
                if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                    tm.runLater(this::updateChildren);
                }
            } else if ("common|resource.status_changed".equals(type)) {
                tm.runLater(this::updateView);
            }
        }
    }

    public void dispose() {
        AzureEventBus.off("common|resource.refresh", listener);
        AzureEventBus.off("common|resource.status_changed", listener);
        this.updater = null;
    }

    public String getIconPath() {
        final String status = resource.status();
        final String type = resource.getClass().getSimpleName().toLowerCase();
        final String icon = StringUtils.isBlank(status) ? type : String.format("%s-%s", type, status.toLowerCase().trim());
        return String.format("/icons/%s.svg", icon);
    }
}
