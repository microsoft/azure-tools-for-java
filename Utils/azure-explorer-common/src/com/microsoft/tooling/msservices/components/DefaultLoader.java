/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
