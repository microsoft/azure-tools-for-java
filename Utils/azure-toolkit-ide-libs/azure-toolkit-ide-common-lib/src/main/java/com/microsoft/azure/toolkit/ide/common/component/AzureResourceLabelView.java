/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
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
import java.util.Optional;
import java.util.function.Function;

public class AzureResourceLabelView<T extends IAzureBaseResource<?, ?>> implements NodeView {

    public static final AzureIconProvider<IAzureBaseResource<?, ?>> DEFAULT_RESOURCE_ICON_PROVIDER =
            new AzureIconProvider<>(AzureResourceLabelView::getAzureBaseResourceIconPath, AzureResourceLabelView::getStatusModifier);

    @Nonnull
    @Getter
    private final T resource;
    @Getter
    private final String label;
    @Getter
    private String description;
    @Getter
    private AzureIcon icon;
    @Nullable
    @Setter
    @Getter
    private Refresher refresher;

    private final AzureEventBus.EventListener<Object, AzureEvent<Object>> listener;
    private final Function<T, String> descriptionLoader;
    private final AzureIconProvider<? super T> iconProvider;

    public AzureResourceLabelView(@Nonnull T resource) {
        this(resource, IAzureBaseResource::getStatus, DEFAULT_RESOURCE_ICON_PROVIDER);
    }

    public AzureResourceLabelView(@Nonnull T resource, @Nonnull Function<T, String> descriptionLoader,
                                  @Nonnull final AzureIconProvider<? super T> iconProvider) {
        this.resource = resource;
        this.label = resource.getName();
        this.iconProvider = iconProvider;
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        this.descriptionLoader = descriptionLoader;
        this.description = descriptionLoader.apply(resource);
        this.icon = iconProvider.getIconWithoutModifier(resource);
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
        if (source instanceof IAzureBaseResource && ((IAzureBaseResource<?, ?>) source).getId().equals(this.resource.getId())) {
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
                        this.icon = iconProvider.getIcon(resource);
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

    public static <T extends AzResource<?, ?, ?>> String getAzResourceIconPath(T resource) {
        AzResource<?, ?, ?> current = resource;
        final StringBuilder modulePath = new StringBuilder();
        while (!(current instanceof AzResource.None)) {
            modulePath.insert(0, "/" + current.getModule().getName());
            current = current.getParent();
        }
        return String.format("/icons%s/default.svg", modulePath);
    }

    public static <T extends IAzureBaseResource<?, ?>> String getAzureBaseResourceIconPath(T resource) {
        if (resource instanceof AzResource) {
            return getAzResourceIconPath((AzResource) resource);
        }
        return String.format("/icons/%s/default.svg", resource.getClass().getSimpleName().toLowerCase());
    }

    public static <T extends IAzureBaseResource<?, ?>> AzureIcon.Modifier getStatusModifier(T resource) {
        return new AzureIcon.Modifier(resource.status().toLowerCase(), AzureIcon.ModifierLocation.BOTTOM_RIGHT);
    }

    @Override
    public String getIconPath() {
        return Optional.ofNullable(getIcon()).map(AzureIcon::getIconPath).orElse(StringUtils.EMPTY);
    }
}
