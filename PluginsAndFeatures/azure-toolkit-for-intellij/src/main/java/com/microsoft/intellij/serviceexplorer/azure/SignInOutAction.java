/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.serviceexplorer.azure;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.actions.AzureSignInAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_IN;
import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.SIGN_OUT;

public class SignInOutAction extends NodeAction {

    SignInOutAction(AzureModule azureModule) {
        super(azureModule, "Sign In/Out");
        addListener(new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                AzureSignInAction.authActionPerformed((Project) azureModule.getProject());
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
            return IdeAzureAccount.getInstance().isLoggedIn() ? "Sign Out" : "Sign In";
        } catch (final Exception e) {
            AzurePlugin.log("Error signing in", e);
            return "";
        }
    }

    @Override
    public AzureIcon getIconSymbol() {
        return getIcon();
    }

    public static AzureIcon getIcon() {
        try {
            return IdeAzureAccount.getInstance().isLoggedIn() ? SIGN_OUT : SIGN_IN;
        } catch (final Exception e) {
            return SIGN_IN;
        }
    }

}
