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
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IntelliJAzureIcons {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";

    private static final Map<String, Icon> icons = new ConcurrentHashMap<>() {
        {
            put("/icons/module", AllIcons.Nodes.Module);
            put("/icons/spinner", AnimatedIcon.Default.INSTANCE);
            put("/icons/error", AllIcons.General.Error);
            put("/icons/unknown", AllIcons.Nodes.Unknown);
        }
    };
    private static final Map<AzureIcon, Icon> azureIcons = new ConcurrentHashMap<>() {
        {
            put(AzureIcons.Common.REFRESH_ICON, AnimatedIcon.Default.INSTANCE);
            put(AzureIcons.Common.FAVORITE, AllIcons.Nodes.Favorite);
            put(AzureIcons.Action.PIN, AllIcons.Nodes.Favorite);
            put(AzureIcons.Action.UNPIN, AllIcons.Nodes.NotFavoriteOnHover);
            put(AzureIcons.Action.START, AllIcons.Actions.Execute);
            put(AzureIcons.Action.STOP, AllIcons.Actions.Suspend);
            put(AzureIcons.Action.RESTART, AllIcons.Actions.Restart);
            put(AzureIcons.Action.REFRESH, AllIcons.Actions.Refresh);
            put(AzureIcons.Action.DEPLOY, AllIcons.Nodes.Deploy);
            put(AzureIcons.Action.CREATE, AllIcons.General.Add);
            put(AzureIcons.Action.DELETE, AllIcons.Actions.GC);
            put(AzureIcons.Action.ADD, AllIcons.General.Add);
            put(AzureIcons.Action.REMOVE, AllIcons.Actions.GC);
            put(AzureIcons.Action.EDIT, AllIcons.Actions.Edit);
            put(AzureIcons.Action.PROPERTIES, AllIcons.Actions.Properties);
            put(AzureIcons.Action.SELECT_SUBSCRIPTION, AllIcons.General.Filter);
            put(AzureIcons.Common.SELECT_SUBSCRIPTIONS, AllIcons.General.Filter);
            put(AzureIcons.Common.DELETE, AllIcons.Actions.GC);
            put(AzureIcons.Common.RESTART, AllIcons.Actions.Restart);
            put(AzureIcons.Common.SHOW_PROPERTIES, AllIcons.Actions.Properties);
            put(AzureIcons.Common.UNKNOWN_ICON, AllIcons.Nodes.Unknown);
            put(AzureIcons.Common.HIDE, AllIcons.General.InspectionsEye);
            put(AzureIcons.Action.CONSOLE, AllIcons.Nodes.Console);
            put(AzureIcons.Action.SFTP, AllIcons.Nodes.Servlet);
            put(AzureIcons.Action.UPLOAD, AllIcons.Actions.Upload);
            put(AzureIcons.Action.DOWNLOAD, AllIcons.Actions.Download);
            put(AzureIcons.Action.COPY, AllIcons.Actions.Copy);
            put(AzureIcons.Action.ATTACH_DEBUGGER, AllIcons.Debugger.AttachToProcess);
            put(AzureIcons.Action.REMOTE_DEBUG, AllIcons.RunConfigurations.RemoteDebug);
            put(AzureIcons.Action.SEARCH, AllIcons.Actions.Search);
            put(AzureIcons.Action.LOG, AllIcons.Nodes.LogFolder);
            put(AzureIcons.AppService.APP_SETTING, AllIcons.Nodes.Variable);
            put(AzureIcons.AppService.APP_SETTINGS, AllIcons.Actions.Properties);
            put(AzureIcons.Common.SERVICES, AllIcons.Toolwindows.ToolWindowServices);
        }
    };

    static {
        azureIcons.forEach((key, value) -> icons.put(key.getIconPath(), value));
    }

    public static Icon getIcon(@Nonnull String iconPathOrName) {
        return getIcon(iconPathOrName, IntelliJAzureIcons.class);
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
        if (StringUtils.isBlank(iconPathOrName)) {
            return null;
        }
        try {
            final Icon result = IconLoader.getIcon(iconPathOrName, clazz);
            return result.getIconHeight() > 1 ? result : null; // IconLoader may return dot for non-existing icon
        } catch (final Throwable ise) {
            return null;
        }
    }

    public static Icon getIcon(@Nonnull AzureIcon azureIcon) {
        return azureIcons.computeIfAbsent(azureIcon, IntelliJAzureIcons::getAzureIcon);
    }

    private static Icon getAzureIcon(@Nonnull AzureIcon azureIcon) {
        return doGetIcon(AzureIcon.getIconPathWithModifier(azureIcon), azureIcon.getIconPath(), IntelliJAzureIcons.class);
    }
}
