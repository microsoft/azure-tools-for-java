/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import rx.subjects.PublishSubject;

public class CosmosSparkDestroyAction extends NodeActionListener {
    @NotNull
    private final DestroyableCluster cluster;
    @NotNull
    private final AzureSparkServerlessAccount adlAccount;
    @NotNull
    private final PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, CosmosSparkClusterNode>> destroyAction;
    @NotNull
    private final CosmosSparkClusterNode clusterNode;

    public CosmosSparkDestroyAction(@NotNull CosmosSparkClusterNode clusterNode,
                                    @NotNull DestroyableCluster cluster,
                                    @NotNull AzureSparkServerlessAccount adlAccount,
                                    @NotNull PublishSubject<Triple<
                                                AzureSparkServerlessAccount,
                                                DestroyableCluster,
                                                CosmosSparkClusterNode>> destroyAction) {
        super();
        this.clusterNode = clusterNode;
        this.adlAccount = adlAccount;
        this.cluster = cluster;
        this.destroyAction = destroyAction;
    }

    @Override
    protected void actionPerformed(NodeActionEvent e) {
        destroyAction.onNext(ImmutableTriple.of(adlAccount, cluster, clusterNode));
    }
}
