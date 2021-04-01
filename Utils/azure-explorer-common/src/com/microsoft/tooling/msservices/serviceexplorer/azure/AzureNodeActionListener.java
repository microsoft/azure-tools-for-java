/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListenerAsync;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;

import java.util.concurrent.Callable;

public abstract class AzureNodeActionListener extends NodeActionListenerAsync {
    protected Node azureNode;

    public AzureNodeActionListener(@NotNull Node azureNode,
                                   @NotNull String progressMessage) {
        super(progressMessage);
        this.azureNode = azureNode;
    }

    @NotNull
    @Override
    protected Callable<Boolean> beforeAsyncActionPerformed() {
        return () -> true;
    }

    @Override
    protected void actionPerformed(final NodeActionEvent e) throws AzureCmdException {
        azureNodeAction(e);
    }

    protected abstract void azureNodeAction(NodeActionEvent e)
            throws AzureCmdException;

    protected abstract void onSubscriptionsChanged(NodeActionEvent e)
            throws AzureCmdException;
}
