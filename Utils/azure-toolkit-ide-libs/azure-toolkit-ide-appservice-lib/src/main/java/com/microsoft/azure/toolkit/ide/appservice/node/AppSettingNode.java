/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.node;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class AppSettingNode extends Node<Map.Entry<String, String>> {
    @Getter
    private boolean visible = false;

    public AppSettingNode(@NotNull Map.Entry<String, String> data) {
        super(data);
        this.view(new AppSettingNodeView(this));
    }

    @Override
    public void triggerClickAction(Object event) {
        this.visible = !this.visible;
        this.view().refreshView();
    }

    private static class AppSettingNodeView implements com.microsoft.azure.toolkit.ide.common.component.NodeView {
        @Nonnull
        @Getter
        private final AppSettingNode value;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public AppSettingNodeView(@Nonnull AppSettingNode node) {
            this.value = node;
            this.refreshView();
        }

        @Override
        public String getLabel() {
            return value.isVisible() ? value.data().getKey() + " = " + value.data().getValue() : value.data().getKey() + " = ***";
        }

        @Override
        public String getIconPath() {
            return AzureIcons.AppService.APP_SETTING.getIconPath();
        }

        @Override
        public String getDescription() {
            return StringUtils.EMPTY;
        }

        @Override
        public void dispose() {

        }
    }
}
