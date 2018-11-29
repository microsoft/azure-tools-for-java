package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class CosmosSparkMonitorAction extends NodeActionListener {
    @NotNull
    private final AzureSparkServerlessCluster cluster;
    @NotNull
    private final CosmosSparkClusterNode clusterNode;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, CosmosSparkClusterNode>> monitorAction;

    public CosmosSparkMonitorAction(@NotNull CosmosSparkClusterNode clusterNode,
                                    @NotNull AzureSparkServerlessCluster cluster,
                                    @NotNull PublishSubject<Pair<
                                                AzureSparkServerlessCluster,
                                                CosmosSparkClusterNode>> monitorAction) {
        super(clusterNode);
        this.cluster = cluster;
        this.clusterNode = clusterNode;
        this.monitorAction = monitorAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) throws AzureCmdException {
        monitorAction.onNext(Pair.of(cluster, clusterNode));
    }
}
