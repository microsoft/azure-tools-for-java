/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureIcons {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";

    private static final Map<String, Icon> icons = new ConcurrentHashMap<>() {
        {
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
        }
    };

    public static Icon getIcon(@Nonnull String iconPathOrName, Class<?> clazz) {
        if (StringUtils.startsWith(iconPathOrName, FILE_EXTENSION_ICON_PREFIX)) {
            final String fileExtension = StringUtils.substringAfter(iconPathOrName, FILE_EXTENSION_ICON_PREFIX);
            return getFileTypeIcon(fileExtension);
        }
        return icons.computeIfAbsent(iconPathOrName, n -> IconLoader.getIcon(n, clazz));
    }

    public static Icon getIcon(@Nonnull String iconPathOrName) {
        return icons.computeIfAbsent(iconPathOrName, n -> IconLoader.getIcon(n, AzureIcons.class));
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
}
