package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.Pair;
import rx.subjects.PublishSubject;

public class SparkServerlessMonitorAction extends NodeActionListener {
    @NotNull
    private final AzureSparkServerlessCluster cluster;
    @NotNull
    private final SparkServerlessClusterNode clusterNode;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> monitorAction;

    public SparkServerlessMonitorAction(@NotNull SparkServerlessClusterNode clusterNode,
                                        @NotNull AzureSparkServerlessCluster cluster,
                                        @NotNull PublishSubject<Pair<
                                                AzureSparkServerlessCluster,
                                                SparkServerlessClusterNode>> monitorAction) {
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
