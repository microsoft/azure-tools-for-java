/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode;

import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import rx.subjects.PublishSubject;

public class CosmosSparkClusterOps {
    private static CosmosSparkClusterOps instance = new CosmosSparkClusterOps();

    @NotNull
    private final PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, CosmosSparkClusterNode>> destroyAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> provisionAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> monitorAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> updateAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> submitAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> serverlessSubmitAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> viewServerlessJobsAction;

    private CosmosSparkClusterOps() {
        destroyAction = PublishSubject.create();
        provisionAction = PublishSubject.create();
        monitorAction = PublishSubject.create();
        updateAction = PublishSubject.create();
        submitAction = PublishSubject.create();
        serverlessSubmitAction = PublishSubject.create();
        viewServerlessJobsAction = PublishSubject.create();
    }

    @NotNull
    public static CosmosSparkClusterOps getInstance() {
        return instance;
    }

    @NotNull
    public PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, CosmosSparkClusterNode>> getDestroyAction() {
        return destroyAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> getProvisionAction() {
        return provisionAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> getMonitorAction() {
        return monitorAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> getUpdateAction() {
        return updateAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkCosmosCluster, CosmosSparkClusterNode>> getSubmitAction() {
        return submitAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> getServerlessSubmitAction() {
        return serverlessSubmitAction;
    }

    public PublishSubject<Pair<AzureSparkServerlessAccount, CosmosSparkADLAccountNode>> getViewServerlessJobsAction() {
        return viewServerlessJobsAction;
    }
}
