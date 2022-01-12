/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.event.AzureOperationEvent;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureResourceLabelView<T extends IAzureBaseResource<?, ?>> implements NodeView {
    @Nonnull
    @Getter
    private final T resource;
    @Getter
    private final String label;
    @Getter
    private String description;
    @Getter
    private String iconPath;
    @Getter
    private AzureIcon icon;
    @Nullable
    @Setter
    @Getter
    private Refresher refresher;

    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    private final Function<T, String> descriptionLoader;
    private final Function<T, String> iconPathFunction;
    private final List<Function<T, AzureIcon.Modifier>> modifierFunctionList;

    public AzureResourceLabelView(@Nonnull T resource) {
        this(resource, IAzureBaseResource::getStatus, AzureResourceLabelView::getResourceIconPath, Arrays.asList(AzureResourceLabelView::getStatusModifier));
    }

    public AzureResourceLabelView(@Nonnull T resource, @Nonnull Function<T, String> descriptionLoader, @Nonnull final Function<T, String> iconPathFunction,
                                  @Nonnull final List<Function<T, AzureIcon.Modifier>> modifierFunctionList) {
        this.resource = resource;
        this.label = resource.name();
        this.iconPathFunction = iconPathFunction;
        this.modifierFunctionList = modifierFunctionList;
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        this.descriptionLoader = descriptionLoader;
        this.description = descriptionLoader.apply(resource);
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
                    tm.runLater(this::refreshView);
                    if (((AzureOperationEvent) event).getStage() == AzureOperationEvent.Stage.AFTER) {
                        tm.runLater(this::refreshChildren);
                    }
                    break;
                case "common|resource.status_changed":
                case "resource.status_changed.resource":
                    tm.runOnPooledThread(() -> {
                        this.icon = getIcon();
                        this.description = descriptionLoader.apply(resource);
                        tm.runLater(this::refreshView);
                    });
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

    @Override
    public AzureIcon getIcon() {
        final String iconPath = iconPathFunction.apply(this.resource);
        final List<AzureIcon.Modifier> modifiers = modifierFunctionList.stream()
                .map(function -> function.apply(this.resource)).filter(Objects::nonNull).collect(Collectors.toList());
        return AzureIcon.builder().iconPath(iconPath).modifierList(modifiers).build();
    }

    public static <T extends IAzureBaseResource<?, ?>> String getResourceIconPath(T resource) {
        return String.format("/icons/%s.svg", resource.getClass().getSimpleName().toLowerCase());
    }

    public static <T extends IAzureBaseResource<?, ?>> AzureIcon.Modifier getStatusModifier(T resource) {
        return new AzureIcon.Modifier(resource.status(), AzureIcon.ModifierLocation.BOTTOM_RIGHT);
    }
}
