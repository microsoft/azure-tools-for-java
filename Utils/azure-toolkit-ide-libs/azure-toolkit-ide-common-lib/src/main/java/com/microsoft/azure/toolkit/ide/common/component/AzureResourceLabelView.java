/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Debouncer;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider.DEFAULT_AZURE_RESOURCE_ICON_PROVIDER;

public class AzureResourceLabelView<T extends AzResource<?, ?, ?>> implements NodeView {

    @Nonnull
    @Getter
    protected final T resource;
    @Getter
    @Setter(value = AccessLevel.PROTECTED)
    private String label;
    @Getter
    private String description;
    @Getter
    private AzureIcon icon;
    @Nullable
    @Setter
    @Getter
    private Refresher refresher;
    @Getter
    private boolean enabled = true;
    private final Debouncer refreshViewInner = new TailingDebouncer(this::refreshViewInner, 300);

    private final AzureEventBus.EventListener listener;
    private final Function<T, String> descriptionLoader;
    private final AzureIconProvider<? super T> iconProvider;

    public AzureResourceLabelView(@Nonnull T resource) {
        this(resource, AzResource::getStatus, DEFAULT_AZURE_RESOURCE_ICON_PROVIDER);
    }

    public AzureResourceLabelView(@Nonnull T resource, @Nonnull Function<T, String> descriptionLoader,
                                  @Nonnull final AzureIconProvider<? super T> iconProvider) {
        this.resource = resource;
        this.label = resource.getName();
        this.iconProvider = iconProvider;
        this.descriptionLoader = descriptionLoader;
        this.listener = new AzureEventBus.EventListener(this::onEvent);
        this.icon = AzureIcons.Common.REFRESH_ICON;
        AzureEventBus.on("resource.refreshed.resource", listener);
        AzureEventBus.on("resource.status_changed.resource", listener);
        AzureEventBus.on("resource.children_changed.resource", listener);
        this.refreshViewInner.debounce();
    }

    public void onEvent(AzureEvent event) {
        final String type = event.getType();
        final Object source = event.getSource();
        if (source instanceof AzResource &&
            ((AzResource<?, ?, ?>) source).getId().equals(this.resource.getId()) &&
            ((AzResource<?, ?, ?>) source).getName().equals(this.resource.getName())) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            if (StringUtils.equals(type, "resource.refreshed.resource")) {
                this.refreshViewInner.debounce();
                tm.runLater(() -> this.refreshChildren(false));
            } else if (StringUtils.equals(type, "resource.status_changed.resource")) {
                this.refreshViewInner.debounce();
            } else if (StringUtils.equals(type, "resource.children_changed.resource")) {
                tm.runLater(() -> this.refreshChildren(true));
            }
        }
    }

    private void refreshViewInner() {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> {
            this.icon = iconProvider.getIcon(this.resource);
            this.description = descriptionLoader.apply(this.resource);
            this.enabled = !this.resource.getFormalStatus().isDeleted();
            tm.runLater(this::refreshView);
        });
    }

    public void dispose() {
        AzureEventBus.off("resource.refreshed.resource", listener);
        AzureEventBus.off("resource.status_changed.resource", listener);
        AzureEventBus.off("resource.children_changed.resource", listener);
        this.refresher = null;
    }

    @Override
    public String getIconPath() {
        return Optional.ofNullable(getIcon()).map(AzureIcon::getIconPath).orElse(StringUtils.EMPTY);
    }
}
