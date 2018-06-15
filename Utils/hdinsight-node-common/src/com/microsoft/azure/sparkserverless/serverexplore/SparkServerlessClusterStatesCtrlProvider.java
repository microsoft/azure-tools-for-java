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

    public Observable<SparkServerlessClusterStatesModel> updateAll() {
        // refresh cluster property
        return cluster.get()
                .observeOn(ideSchedulers.processBarVisibleAsync("Updating cluster status..."))
                .map(clusterUpdated -> {
                    SparkServerlessClusterStatesModel toUpdate = new SparkServerlessClusterStatesModel();
                    controllableView.getData(toUpdate);

                    String suffix = "/?adlaAccountName=" + cluster.getAccount().getName();
                    return toUpdate.setMasterState(Optional.ofNullable(cluster.getMasterState()).orElse("Unknown"))
                            .setWorkerState(Optional.ofNullable(cluster.getWorkerState()).orElse("Unknown"))
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
                            .setClusterState(cluster.getState());
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData);
    }

}
