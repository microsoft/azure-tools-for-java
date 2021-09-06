/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureServiceLabelView<T extends AzureService> implements IView.Label, IView.Dynamic {
    @Nonnull
    @Getter
    private final T service;
    @Getter
    private final String label;
    @Getter
    private final String iconPath;
    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    @Getter
    private String description;
    @Nullable
    @Setter
    @Getter
    private Updater updater;

    public AzureServiceLabelView(@Nonnull T service) {
        this(service, service.name());
    }

    public AzureServiceLabelView(@Nonnull T service, String label) {
        this(service, label, String.format("/icons/%s.svg", service.getClass().getSimpleName().toLowerCase()));
    }

    public AzureServiceLabelView(@Nonnull T service, String label, String iconPath) {
        this.service = service;
        this.label = label;
        this.iconPath = iconPath;
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("common|service.refresh", listener);
        this.updateView();
    }

    public void dispose() {
        AzureEventBus.off("common|service.refresh", listener);
        this.updater = null;
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if ("common|service.refresh".equals(type)
                && source instanceof AzureService
                && ((AzureService) source).name().equals(this.service.name())) {
            if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                AzureTaskManager.getInstance().runLater(this::updateChildren);
            }
        }
    }
}
