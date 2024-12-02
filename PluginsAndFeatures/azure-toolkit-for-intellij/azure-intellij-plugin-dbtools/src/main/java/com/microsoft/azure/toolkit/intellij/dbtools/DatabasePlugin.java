/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;

public class DatabasePlugin {
    private static final String DATABASE_TOOLS_PLUGIN_ID = "com.intellij.database";
    private static final String DATABASE_PLUGIN_NOT_INSTALLED = "\"Database tools and SQL\" plugin is not installed.";
    private static final String NOT_SUPPORT_ERROR_ACTION = "\"Database tools and SQL\" plugin is only provided in IntelliJ Ultimate edition.";

    public static boolean isInstalled() {
        return PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null;
    }

    public static void throwTryUltimateIfNotInstalled(Object binder) {
        if (PluginManagerCore.getPlugin(PluginId.findId(DATABASE_TOOLS_PLUGIN_ID)) == null) {
            final Action<Object> tryUltimate = AzureActionManager.getInstance().getAction(IntellijActionsContributor.TRY_ULTIMATE).bind(binder);
            throw new AzureToolkitRuntimeException(DATABASE_PLUGIN_NOT_INSTALLED, NOT_SUPPORT_ERROR_ACTION, tryUltimate);
        }
    }
}
