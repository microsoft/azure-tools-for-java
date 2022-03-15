/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.component;

import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class AzureModuleLabelView<T extends AzResourceModule<?, ?, ?>> implements NodeView {
    @Nonnull
    @Getter
    private final T module;
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

    public AzureModuleLabelView(@Nonnull T module) {
        this(module, module.getName());
    }

    public AzureModuleLabelView(@Nonnull T module, String label) {
        this(module, label, String.format("/icons/%s.svg", module.getClass().getSimpleName().toLowerCase()));
    }

    public AzureModuleLabelView(@Nonnull T module, String label, String iconPath) {
        this.module = module;
        this.label = label;
        this.iconPath = iconPath;
        this.listener = new AzureEventBus.EventListener<>(this::onEvent);
        AzureEventBus.on("module.refreshed.module", listener);
        AzureEventBus.on("module.children_changed.module", listener);
        this.refreshView();
    }

    public void dispose() {
        AzureEventBus.off("module.refreshed.module", listener);
        AzureEventBus.off("module.children_changed.module", listener);
        this.refresher = null;
    }

    public void onEvent(AzureEvent<Object> event) {
        final String type = event.getType();
        final Object source = event.getSource();
        final boolean childrenChanged = StringUtils.equalsIgnoreCase(type, "module.children_changed.module");
        if (source instanceof AzResourceModule) {
            final AzureTaskManager tm = AzureTaskManager.getInstance();
            if (Objects.equals(source, this.module)) {
                tm.runLater(() -> this.refreshChildren(childrenChanged));
            }
        }
    }
}
