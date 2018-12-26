package com.microsoft.azure.cosmosspark.serverexplore;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.common.SparkAzureDataLakePoolServiceException;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

public class CosmosSparkClusterUpdateCtrlProvider implements ILogger {
    @NotNull
    private SettableControl<CosmosSparkClusterProvisionSettingsModel> controllableView;
    @NotNull
    private IdeSchedulers ideSchedulers;
    @NotNull
    private AzureSparkServerlessCluster cluster;

    public CosmosSparkClusterUpdateCtrlProvider(
            @NotNull SettableControl<CosmosSparkClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkServerlessCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<CosmosSparkClusterProvisionSettingsModel> initialize() {
        return Observable.just(cluster)
                .observeOn(ideSchedulers.processBarVisibleAsync("Updating cluster status..."))
                .flatMap(cluster -> cluster.get())
                .map(clusterUpdated -> {
                    CosmosSparkClusterProvisionSettingsModel toUpdate =
                            new CosmosSparkClusterProvisionSettingsModel();
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
                            .setCalculatedAU(CosmosSparkClusterProvisionCtrlProvider.getCalculatedAU(
                                    clusterUpdated.getMasterPerInstanceCoreCount(),
                                    clusterUpdated.getWorkerPerInstanceCoreCount(),
                                    clusterUpdated.getMasterPerInstanceMemoryInGB(),
                                    clusterUpdated.getWorkerPerInstanceMemoryInGB(),
                                    clusterUpdated.getWorkerTargetInstanceCount()))
                            .setClusterGuid(clusterUpdated.getGuid());
                })
                .doOnNext(controllableView::setData);
    }

    public Observable<CosmosSparkClusterProvisionSettingsModel> validateAndUpdate() {
        return Observable.just(new CosmosSparkClusterProvisionSettingsModel())
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
                                    return toUpdate
                                            .setClusterGuid(cluster.getGuid())
                                            .setErrorMessage(err.getMessage());
                                }))
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
