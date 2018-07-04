package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.net.URI;
import java.util.Optional;

public class SparkServerlessClusterStatesCtrlProvider {
    @NotNull
    private SettableControl<SparkServerlessClusterStatesModel> controllableView;
    @NotNull
    private IdeSchedulers ideSchedulers;
    @NotNull
    private AzureSparkServerlessCluster cluster;

    public SparkServerlessClusterStatesCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterStatesModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkServerlessCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<AzureSparkServerlessCluster> updateAll() {

        return Observable.just(new SparkServerlessClusterStatesModel())
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
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .observeOn(Schedulers.io())
                .flatMap(data -> cluster.get());
    }

}
