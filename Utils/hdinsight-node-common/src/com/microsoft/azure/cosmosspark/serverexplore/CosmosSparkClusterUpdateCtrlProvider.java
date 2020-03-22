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
import com.microsoft.azure.hdinsight.sdk.common.SparkAzureDataLakePoolServiceException;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Observable;

public class CosmosSparkClusterUpdateCtrlProvider implements ILogger {
    @NotNull
    private SettableControl<CosmosSparkClusterProvisionSettingsModel> controllableView;
    @NotNull
    private IdeSchedulers ideSchedulers;
    @NotNull
    private AzureSparkCosmosCluster cluster;

    public CosmosSparkClusterUpdateCtrlProvider(
            @NotNull SettableControl<CosmosSparkClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkCosmosCluster cluster) {
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
                                .doOnNext(model -> {
                                    // Send telemetry when update cluster succeeded
                                    AzureSparkCosmosClusterManager.getInstance().sendInfoTelemetry(
                                            TelemetryConstants.UPDATE_A_CLUSTER, cluster.getGuid());
                                })
                                .onErrorReturn(err -> {
                                    log().warn("Error update a cluster. " + ExceptionUtils.getStackTrace(err));
                                    if (err instanceof SparkAzureDataLakePoolServiceException) {
                                        String requestId = ((SparkAzureDataLakePoolServiceException) err).getRequestId();
                                        toUpdate.setRequestId(requestId);
                                        log().info("x-ms-request-id: " + requestId);
                                    }
                                    log().info("Cluster guid: " + cluster.getGuid());
                                    // Send telemetry when update cluster failed
                                    AzureSparkCosmosClusterManager.getInstance().sendErrorTelemetry(
                                            TelemetryConstants.UPDATE_A_CLUSTER, err, cluster.getGuid());
                                    return toUpdate
                                            .setClusterGuid(cluster.getGuid())
                                            .setErrorMessage(err.getMessage());
                                }))
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
