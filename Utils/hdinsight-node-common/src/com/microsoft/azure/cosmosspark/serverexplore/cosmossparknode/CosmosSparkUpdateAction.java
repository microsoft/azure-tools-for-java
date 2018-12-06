package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkUpdateAction extends NodeActionListener {
    @NotNull
    private final AzureSparkServerlessCluster cluster;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, CosmosSparkClusterNode>> updateAction;
    private final CosmosSparkClusterNode clusterNode;

    public CosmosSparkUpdateAction(@NotNull CosmosSparkClusterNode clusterNode,
                                   @NotNull AzureSparkServerlessCluster cluster,
                                   @NotNull PublishSubject<Pair<AzureSparkServerlessCluster,
                                               CosmosSparkClusterNode>> updateAction) {
        super(clusterNode);
        this.cluster = cluster;
        this.clusterNode = clusterNode;
        this.updateAction = updateAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) {
        updateAction.onNext(ImmutablePair.of(cluster, clusterNode));
    }
}
