/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.google.common.base.Preconditions;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AzureIconLoader {

    private static final Map<AzureIconSymbol, Icon> ICON_MAP = new ConcurrentHashMap<>();

    static {
        loadIDEIcons();
    }

    public static Icon loadIcon(AzureIconSymbol iconSymbol) {
        Preconditions.checkNotNull(iconSymbol);
        Preconditions.checkArgument(StringUtils.isNotBlank(iconSymbol.getPath()), "iconPath can not be blank.");
        return ICON_MAP.computeIfAbsent(iconSymbol, k -> IconLoader.getIcon(iconSymbol.getPath()));
    }

    private static void loadIDEIcons() {
        loadIcon(AzureIconSymbol.Common.REFRESH, AllIcons.Actions.Refresh);
        loadIcon(AzureIconSymbol.Common.DELETE, AllIcons.Actions.GC);
        loadIcon(AzureIconSymbol.Common.RESTART, AllIcons.Actions.Restart);
        loadIcon(AzureIconSymbol.Common.SHOW_PROPERTIES, AllIcons.Actions.Properties);
    }

    private static Icon loadIcon(AzureIconSymbol iconSymbol, Icon customIcon) {
        Preconditions.checkNotNull(iconSymbol);
        Preconditions.checkArgument(StringUtils.isNotBlank(iconSymbol.getPath()), "iconPath can not be blank.");
        Preconditions.checkNotNull(customIcon);
        return ICON_MAP.computeIfAbsent(iconSymbol, k -> customIcon);
    }

}
