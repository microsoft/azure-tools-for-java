/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkSubmitAction extends NodeActionListener {
    @NotNull
    private final AzureSparkCosmosCluster cluster;
    @NotNull
    private final PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> submitAction;
    private final CosmosSparkClusterNode clusterNode;

    public CosmosSparkSubmitAction(@NotNull CosmosSparkClusterNode clusterNode,
                                   @NotNull AzureSparkCosmosCluster cluster,
                                   @NotNull PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> submitAction) {
        super();
        this.cluster = cluster;
        this.clusterNode = clusterNode;
        this.submitAction = submitAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) {
        submitAction.onNext(ImmutablePair.of(cluster, clusterNode));
    }
}
