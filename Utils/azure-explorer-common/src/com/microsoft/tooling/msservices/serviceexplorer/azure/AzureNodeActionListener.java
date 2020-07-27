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
