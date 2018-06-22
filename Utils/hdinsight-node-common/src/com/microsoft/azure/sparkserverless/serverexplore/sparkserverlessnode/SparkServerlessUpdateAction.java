package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class SparkServerlessUpdateAction extends NodeActionListener {
    @NotNull
    private final AzureSparkServerlessCluster cluster;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> updateAction;
    private final SparkServerlessClusterNode clusterNode;

    public SparkServerlessUpdateAction(@NotNull SparkServerlessClusterNode clusterNode,
                                       @NotNull AzureSparkServerlessCluster cluster,
                                       @NotNull PublishSubject<Pair<AzureSparkServerlessCluster,
                                               SparkServerlessClusterNode>> updateAction) {
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
