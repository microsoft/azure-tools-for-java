/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AzureServiceLabelView<T extends AzService> implements NodeView {
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
    private Refresher refresher;

    public AzureServiceLabelView(@Nonnull T service) {
        this(service, service.getName());
    }

    public AzureServiceLabelView(@Nonnull T service, String label) {
        this(service, label, String.format("/icons/%s.svg", service.getClass().getSimpleName().toLowerCase()));
    }

    public AzureServiceLabelView(@Nonnull T service, String label, String iconPath) {
        this.service = service;
        this.label = label;
        this.iconPath = iconPath;
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("service.refresh.service", listener);
        AzureEventBus.on("service.children_changed.service", listener);
        this.refreshView();
    }

    public void dispose() {
        AzureEventBus.off("service.refresh.service", listener);
        AzureEventBus.off("service.children_changed.service", listener);
        this.refresher = null;
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof AzService && ((AzService) source).getName().equals(this.service.getName())) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            switch (type) {
                case "service.refresh.service":
                    if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                        tm.runLater(this::refreshChildren);
                    }
                    break;
                case "service.children_changed.service":
                    tm.runLater(this::refreshChildren);
                    break;
            }
        }
    }
}
