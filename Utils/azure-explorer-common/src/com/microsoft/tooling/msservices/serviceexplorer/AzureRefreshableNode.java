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
package com.microsoft.tooling.msservices.serviceexplorer;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public abstract class AzureRefreshableNode extends RefreshableNode {
    public AzureRefreshableNode(String id, String name, Node parent, String iconPath) {
        super(id, name, parent, iconPath);
    }

    public AzureRefreshableNode(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        super(id, name, parent, iconPath, delayActionLoading);
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        try {
            if (AuthMethodManager.getInstance().isSignedIn()) {
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
