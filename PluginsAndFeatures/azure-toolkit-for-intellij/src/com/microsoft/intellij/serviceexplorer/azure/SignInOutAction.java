/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class SignInOutAction extends NodeAction {
    private static final String ICON_SIGNIN_DARK = "SignInDark.svg";
    private static final String ICON_SIGNIN_LIGHT = "SignInLight.svg";
    private static final String ICON_SIGNOUT_DARK = "SignOutDark.svg";
    private static final String ICON_SIGNOUT_LIGHT = "SignOutLight.svg";

    SignInOutAction(AzureModule azureModule) {
        super(azureModule, "Sign In/Out");
        addListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
                AzureSignInAction.onAzureSignIn((Project) azureModule.getProject());
            }

            @Override
            protected void afterActionPerformed(NodeActionEvent e) {
                azureModule.load(false);
            }
        });
    }

    @Override
    public String getName() {
        try {
            return AuthMethodManager.getInstance().isSignedIn() ? "Sign Out" : "Sign In";
        } catch (Exception e) {
            AzurePlugin.log("Error signing in", e);
            return "";
        }
    }

    @Override
    public String getIconPath() {
        return getIcon();
    }

    public static String getIcon() {
        boolean isSignedIn = false;
        try {
            isSignedIn = AuthMethodManager.getInstance().isSignedIn();
        } catch (Exception ex) {}
        if (DefaultLoader.getUIHelper().isDarkTheme()) {
            return isSignedIn ? ICON_SIGNOUT_DARK : ICON_SIGNIN_DARK;
        } else {
            return isSignedIn ? ICON_SIGNOUT_LIGHT : ICON_SIGNIN_LIGHT;
        }
    }
}
