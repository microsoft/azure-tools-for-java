/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public abstract class AzureRefreshableNode extends RefreshableNode {
    public AzureRefreshableNode(String id, String name, Node parent) {
        super(id, name, parent);
    }

    public AzureRefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public AzureRefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        try {
            if (IdeAzureAccount.getInstance().isLoggedIn()) {
                super.onNodeClick(e);
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("Error when expanding node.", ex);
        }
    }

    @Override
    public void removeAllChildNodes() {
        super.removeAllChildNodes();
        // removed everything as a result of subscription change, not during refresh
        if (!loading) {
            initialized = false;
        }
    }
}
