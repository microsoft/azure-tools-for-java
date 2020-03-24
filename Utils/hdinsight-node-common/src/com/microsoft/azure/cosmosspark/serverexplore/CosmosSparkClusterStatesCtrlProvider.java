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

package com.microsoft.azure.cosmosspark.serverexplore;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.net.URI;
import java.util.Optional;

public class CosmosSparkClusterStatesCtrlProvider implements ILogger {
    @NotNull
    private SettableControl<CosmosSparkClusterStatesModel> controllableView;
    @NotNull
    private IdeSchedulers ideSchedulers;
    @NotNull
    private AzureSparkCosmosCluster cluster;

    public CosmosSparkClusterStatesCtrlProvider(
            @NotNull SettableControl<CosmosSparkClusterStatesModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkCosmosCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<AzureSparkCosmosCluster> updateAll() {

        return Observable.just(new CosmosSparkClusterStatesModel())
                .map(toUpdate -> {
                    String suffix = "/?adlaAccountName=" + cluster.getAccount().getName();
                    return toUpdate
                            .setMasterState(
                                    Optional.ofNullable(cluster.getMasterState()).orElse("Unknown").toUpperCase())
                            .setWorkerState(
                                    Optional.ofNullable(cluster.getWorkerState()).orElse("Unknown").toUpperCase())
                            .setMasterTarget(cluster.getMasterTargetInstanceCount())
                            .setWorkerTarget(cluster.getWorkerTargetInstanceCount())
                            .setMasterRunning(cluster.getMasterRunningInstanceCount())
                            .setWorkerRunning(cluster.getWorkerRunningInstanceCount())
                            .setMasterFailed(cluster.getMasterFailedInstanceCount())
                            .setWorkerFailed(cluster.getWorkerFailedInstanceCount())
                            .setMasterOutstanding(cluster.getMasterOutstandingInstanceCount())
                            .setWorkerOutstanding(cluster.getWorkerOutstandingInstanceCount())
                            .setSparkHistoryUri(cluster.getSparkHistoryUiUri() != null
                                    ? URI.create(String.valueOf(cluster.getSparkHistoryUiUri() + suffix)) : null)
                            .setSparkMasterUri(cluster.getSparkMasterUiUri() != null
                                    ? URI.create(String.valueOf(cluster.getSparkMasterUiUri() + suffix)) : null)
                            // cluster state here is set to align with cluster node state
                            .setClusterState(cluster.getMasterState() != null
                                    ? cluster.getMasterState().toUpperCase() : cluster.getState().toUpperCase())
                            .setClusterID(cluster.getGuid());
                })
                .doOnNext(controllableView::setData)
                .observeOn(Schedulers.io())
                .flatMap(data -> cluster.get())
                .onErrorReturn(err -> {
                    log().warn(String.format("Can't get the cluster %s details: %s", cluster.getName(), err));
                    return cluster;
                });
    }

}
