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

package com.microsoft.tooling.msservices.serviceexplorer.azure.sparkserverless;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionListener;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import rx.subjects.PublishSubject;

public class SparkServerlessDestroyAction extends AzureNodeActionListener {
    // TODO: Update clusterName type
    private final String clusterName;
    // TODO: Update adlAccount type
    private final String adlAccount;
    private final PublishSubject<Triple<String, String, Node>> destroyAction;

    public SparkServerlessDestroyAction(@NotNull Node node,
                                        @NotNull String clusterName,
                                        @NotNull String adlAccount,
                                        @NotNull PublishSubject<Triple<String, String, Node>> destroyAction) {
        super(node, "Deleting SparkServerless Cluster");
        this.adlAccount = adlAccount;
        this.clusterName = clusterName;
        this.destroyAction = destroyAction;
    }

    @Override
    protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
        Node currentNode = e.getAction().getNode();
        destroyAction.onNext(ImmutableTriple.of(adlAccount, clusterName, currentNode));
    }

    @Override
    protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
    }
}