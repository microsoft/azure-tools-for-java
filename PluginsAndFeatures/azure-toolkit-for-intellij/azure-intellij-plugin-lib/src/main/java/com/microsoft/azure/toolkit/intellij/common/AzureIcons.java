/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureIcons {
    private static final Map<String, Icon> icons = new ConcurrentHashMap<>() {
        {
            put("/icons/action/restart.svg", AllIcons.Actions.Restart);
            put("/icons/action/start.svg", AllIcons.Actions.Execute);
            put("/icons/action/stop.svg", AllIcons.Actions.Suspend);
            put("/icons/action/refresh.svg", AllIcons.Actions.Refresh);
            put("/icons/action/deploy.svg", AllIcons.Nodes.Deploy);
            put("/icons/action/create.svg", AllIcons.General.Add);
            put("/icons/action/delete.svg", AllIcons.Vcs.Remove);
            put("/icons/action/portal.svg", AllIcons.Actions.Restart);
            put("/icons/action/browser.svg", AllIcons.Actions.InlayGlobe);
            put("/icons/action/properties.svg", AllIcons.Actions.Properties);
        }
    };

    public static Icon getIcon(@Nonnull String iconPathOrName, Class<?> clazz) {
        return icons.computeIfAbsent(iconPathOrName, n -> IconLoader.getIcon(n, clazz));
    }

    public static Icon getIcon(@Nonnull String iconPathOrName) {
        return icons.computeIfAbsent(iconPathOrName, n -> IconLoader.getIcon(n, AzureIcons.class));
    }
}
