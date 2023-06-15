/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.node;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AppSettingsNode extends Node<Supplier<Map<String, String>>> {
    public AppSettingsNode(@NotNull AppServiceAppBase<?,?,?> data) {
        super(data::getAppSettings, new AppSettingsNodeView(data));
        this.addChildren(this::getAppSettingNodes);
    }

    public List<Node<?>> getAppSettingNodes(@Nonnull Supplier<Map<String, String>> supplier) {
        final Map<String, String> appSettings = Optional.ofNullable(supplier.get()).orElse(Collections.emptyMap());
        return appSettings.entrySet()
                .stream().map(AppSettingNode::new).collect(Collectors.toList());
    }

    private static class AppSettingsNodeView implements NodeView {
        @Nonnull
        @Getter
        private final AppServiceAppBase<?,?,?> app;
        private final AzureEventBus.EventListener listener;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public AppSettingsNodeView(@Nonnull AppServiceAppBase<?,?,?> app) {
            this.app = app;
            this.listener = new AzureEventBus.EventListener(this::onEvent);
            AzureEventBus.on("resource.refreshed.resource", listener);
            AzureEventBus.on("resource.status_changed.resource", listener);
            this.refreshView();
        }

        @Override
        public String getLabel() {
            return "App Settings";
        }

        @Override
        public String getIconPath() {
            return AzureIcons.AppService.APP_SETTINGS.getIconPath();
        }

        @Override
        public String getDescription() {
            return "Variables passed as environment variables to the application code";
        }

        @Override
        public void dispose() {
            AzureEventBus.off("resource.refreshed.resource", listener);
            AzureEventBus.off("resource.status_changed.resource", listener);
            this.refresher = null;
        }

        public void onEvent(AzureEvent event) {
            final Object source = event.getSource();
            if (source instanceof AzResource && ((AzResource) source).getId().equals(this.app.getId())) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }
    }
}
