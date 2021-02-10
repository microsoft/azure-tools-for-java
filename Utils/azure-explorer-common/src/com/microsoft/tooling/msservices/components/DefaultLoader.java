/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.components;

import com.microsoft.tooling.msservices.helpers.IDEHelper;
import com.microsoft.tooling.msservices.helpers.UIHelper;

public class DefaultLoader {
    private static UIHelper uiHelper;
    private static IDEHelper ideHelper;
    private static PluginComponent pluginComponent;

    public static void setUiHelper(UIHelper uiHelper) {
        DefaultLoader.uiHelper = uiHelper;
    }

    public static void setPluginComponent(PluginComponent pluginComponent) {
        DefaultLoader.pluginComponent = pluginComponent;
    }

    public static void setIdeHelper(IDEHelper ideHelper) {
        DefaultLoader.ideHelper = ideHelper;
    }

    public static UIHelper getUIHelper() {
        return uiHelper;
    }

    public static PluginComponent getPluginComponent() {
        return pluginComponent;
    }

    public static IDEHelper getIdeHelper() {
        return ideHelper;
    }
}
