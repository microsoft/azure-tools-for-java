/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class SignInOutAction extends NodeAction {
    private static final String ICON_SIGNIN_DARK = "SignInDark_16.png";
    private static final String ICON_SIGNIN_LIGHT = "SignInLight_16.png";
    private static final String ICON_SIGNOUT_DARK = "SignOutDark_16.png";
    private static final String ICON_SIGNOUT_LIGHT = "SignOutLight_16.png";

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
