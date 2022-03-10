/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class AzureIcons {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";

    private static final Map<String, Icon> icons = new ConcurrentHashMap<>() {
        {
            put("/icons/Common/pin.svg", AllIcons.Nodes.Favorite);
            put("/icons/Common/unpin.svg", AllIcons.Nodes.NotFavoriteOnHover);
            put("/icons/Common/favorite.svg", AllIcons.Nodes.Favorite);
            put("/icons/action/restart.svg", AllIcons.Actions.Restart);
            put("/icons/action/start.svg", AllIcons.Actions.Execute);
            put("/icons/action/stop.svg", AllIcons.Actions.Suspend);
            put("/icons/action/refresh.svg", AllIcons.Actions.Refresh);
            put("/icons/action/deploy.svg", AllIcons.Nodes.Deploy);
            put("/icons/action/create.svg", AllIcons.General.Add);
            put("/icons/action/delete.svg", AllIcons.Actions.GC);
            put("/icons/action/portal.svg", IconLoader.getIcon("icons/Common/OpenInPortal.svg", AzureIcons.class));
            put("/icons/action/browser.svg", IconLoader.getIcon("icons/Common/OpenInPortal.svg", AzureIcons.class));
            put("/icons/action/properties.svg", AllIcons.Actions.Properties);
            put("/icons/action/refresh", AllIcons.Actions.Refresh);
            put("/icons/action/add", AllIcons.General.Add);
            put("/icons/action/remove", AllIcons.Actions.GC);
            put("/icons/action/edit", AllIcons.Actions.Edit);
            put("/icons/module", AllIcons.Nodes.Module);
            put("/icons/spinner", AnimatedIcon.Default.INSTANCE);
            put("/icons/error", AllIcons.General.Error);
        }
    };
    private static final Map<AzureIcon, Icon> azureIcons = new ConcurrentHashMap<>() {
        {
            put(AzureIcon.REFRESH_ICON, AnimatedIcon.Default.INSTANCE);
        }
    };

    public static Icon getIcon(@Nonnull String iconPathOrName) {
        return getIcon(iconPathOrName, AzureIcons.class);
    }

    public static Icon getIcon(@Nonnull String iconPathOrName, Class<?> clazz) {
        String fallback = null;
        if (iconPathOrName.contains(":")) {
            final String[] parts = iconPathOrName.split(":");
            iconPathOrName = parts[0];
            if (parts.length > 1) {
                fallback = parts[1];
            }
        }
        return doGetIcon(iconPathOrName, fallback, clazz);
    }

    @Nullable
    private static Icon doGetIcon(@Nonnull String iconPathOrName, @Nullable String fallback, Class<?> clazz) {
        if (StringUtils.startsWith(iconPathOrName, FILE_EXTENSION_ICON_PREFIX)) {
            final String fileExtension = StringUtils.substringAfter(iconPathOrName, FILE_EXTENSION_ICON_PREFIX);
            return getFileTypeIcon(fileExtension);
        }
        return icons.computeIfAbsent(iconPathOrName, path -> Optional.ofNullable(loadIcon(iconPathOrName, clazz))
                .orElseGet(() -> StringUtils.isEmpty(fallback) ? null : loadIcon(fallback, clazz)));
    }

    private static Icon getFileTypeIcon(final String name) {
        if (StringUtils.equalsIgnoreCase(name, "root")) {
            return AllIcons.Nodes.CopyOfFolder;
        }
        if (StringUtils.equalsIgnoreCase(name, "folder")) {
            return AllIcons.Nodes.Folder;
        }
        final FileType type = FileTypeManager.getInstance().getFileTypeByExtension(name);
        return type.getIcon();
    }

    @Nullable
    private static Icon loadIcon(String iconPathOrName, Class<?> clazz) {
        try {
            final Icon result = IconLoader.getIcon(iconPathOrName, clazz);
            return result.getIconHeight() > 1 ? result : null; // IconLoader may return dot for non-existing icon
        } catch (final RuntimeException ise) {
            return null;
        }
    }

    public static Icon getIcon(@Nonnull AzureIcon azureIcon) {
        return azureIcons.computeIfAbsent(azureIcon, AzureIcons::getAzureIcon);
    }

    private static Icon getAzureIcon(@Nonnull AzureIcon azureIcon) {
        return doGetIcon(AzureIcon.getIconPathWithModifier(azureIcon), azureIcon.getIconPath(), AzureIcons.class);
    }
}
