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

package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import rx.subjects.PublishSubject;

public class SparkServerlessDestroyAction extends NodeActionListener {
    @NotNull
    private final DestroyableCluster cluster;
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;
    @NotNull
    private final PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, SparkServerlessClusterNode>> destroyAction;
    @NotNull
    private final SparkServerlessClusterNode clusterNode;

    public SparkServerlessDestroyAction(@NotNull SparkServerlessClusterNode clusterNode,
                                        @NotNull DestroyableCluster cluster,
                                        @NotNull AzureSparkServerlessAccount adlAccount,
                                        @NotNull PublishSubject<Triple<
                                                AzureSparkServerlessAccount,
                                                DestroyableCluster,
                                                SparkServerlessClusterNode>> destroyAction) {
        super(clusterNode);
        this.clusterNode = clusterNode;
        this.adlAccount = adlAccount;
        this.cluster = cluster;
        this.destroyAction = destroyAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        destroyAction.onNext(ImmutableTriple.of(adlAccount, cluster, clusterNode));
    }
}