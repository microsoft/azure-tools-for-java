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
package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkMonitorAction extends NodeActionListener {
    @NotNull
    private final AzureSparkCosmosCluster cluster;
    @NotNull
    private final CosmosSparkClusterNode clusterNode;
    @NotNull
    private final PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> monitorAction;

    public CosmosSparkMonitorAction(@NotNull CosmosSparkClusterNode clusterNode,
                                    @NotNull AzureSparkCosmosCluster cluster,
                                    @NotNull PublishSubject<Pair<
                                            AzureSparkCosmosCluster,
                                                CosmosSparkClusterNode>> monitorAction) {
        super();
        this.cluster = cluster;
        this.clusterNode = clusterNode;
        this.monitorAction = monitorAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        monitorAction.onNext(Pair.of(cluster, clusterNode));
    }
}
