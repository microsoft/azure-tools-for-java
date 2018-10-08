package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.SparkAzureDataLakePoolServiceException;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

public class SparkServerlessClusterUpdateCtrlProvider implements ILogger {
    @NotNull
    private SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView;
    @NotNull
    private IdeSchedulers ideSchedulers;
    @NotNull
    private AzureSparkServerlessCluster cluster;

    public SparkServerlessClusterUpdateCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkServerlessCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<SparkServerlessClusterProvisionSettingsModel> initialize() {
        return Observable.just(cluster)
                .observeOn(ideSchedulers.processBarVisibleAsync("Updating cluster status..."))
                .flatMap(cluster -> cluster.get())
                .map(clusterUpdated -> {
                    SparkServerlessClusterProvisionSettingsModel toUpdate =
                            new SparkServerlessClusterProvisionSettingsModel();
                    controllableView.getData(toUpdate);
                    return toUpdate
                            .setClusterName(clusterUpdated.getName())
                            .setAdlAccount(clusterUpdated.getAccount().getName())
                            .setSparkEvents(clusterUpdated.getSparkEventsPath())
                            // TODO: set available AU
                            .setTotalAU(clusterUpdated.getAccount().getSystemMaxDegreeOfParallelism())
                            .setMasterCores(clusterUpdated.getMasterPerInstanceCoreCount())
                            .setMasterMemory(clusterUpdated.getMasterPerInstanceMemoryInGB())
                            .setWorkerCores(clusterUpdated.getWorkerPerInstanceCoreCount())
                            .setWorkerMemory(clusterUpdated.getWorkerPerInstanceMemoryInGB())
                            .setWorkerNumberOfContainers(clusterUpdated.getWorkerTargetInstanceCount())
                            .setCalculatedAU(SparkServerlessClusterProvisionCtrlProvider.getCalculatedAU(
                                    clusterUpdated.getMasterPerInstanceCoreCount(),
                                    clusterUpdated.getWorkerPerInstanceCoreCount(),
                                    clusterUpdated.getMasterPerInstanceMemoryInGB(),
                                    clusterUpdated.getWorkerPerInstanceMemoryInGB(),
                                    clusterUpdated.getWorkerTargetInstanceCount()));
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData);
    }

    public Observable<SparkServerlessClusterProvisionSettingsModel> validateAndUpdate() {
        return Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Updating cluster..."))
                .map(toUpdate -> toUpdate.setErrorMessage(null))
                .flatMap(toUpdate ->
                        cluster.update(toUpdate.getWorkerNumberOfContainers())
                                .map(cluster -> toUpdate)
                                .onErrorReturn(err -> {
                                    log().warn("Error update a cluster. " + ExceptionUtils.getStackTrace(err));
                                    if (err instanceof SparkAzureDataLakePoolServiceException) {
                                        String requestId = ((SparkAzureDataLakePoolServiceException) err).getRequestId();
                                        toUpdate.setRequestId(requestId);
                                        log().info("x-ms-request-id: " + requestId);
                                    }
                                    log().info("Cluster guid: " + cluster.getGuid());
                                    return toUpdate.setErrorMessage(err.getMessage());
                                }))
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
