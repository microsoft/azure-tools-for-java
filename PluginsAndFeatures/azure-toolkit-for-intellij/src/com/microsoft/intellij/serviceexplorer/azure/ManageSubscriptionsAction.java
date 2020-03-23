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

package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class ManageSubscriptionsAction extends NodeAction {
    private static final String ICON_DARK = "ConnectAccountsDark_16.png";
    private static final String ICON_LIGHT = "ConnectAccountsLight_16.png";

    public ManageSubscriptionsAction(AzureModule azureModule) {
        super(azureModule, "Select Subscriptions");
        addListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                SelectSubscriptionsAction.onShowSubscriptions((Project) azureModule.getProject());
            }
        });
    }

    @Override
    public String getIconPath() {
        return getIcon();
    }

    public static String getIcon () {
        return DefaultLoader.getUIHelper().isDarkTheme() ? ICON_DARK : ICON_LIGHT;
    }

    public boolean isEnabled() {
        try {
            return super.isEnabled() && AuthMethodManager.getInstance().isSignedIn();
        } catch (Exception e) {
            AzurePlugin.log("Error signing in", e);
            return false;
        }
    }
}