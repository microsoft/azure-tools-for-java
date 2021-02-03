/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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

public class CosmosSparkClusterDestoryCtrlProvider implements ILogger {
    @NotNull
    private SettableControl<CosmosSparkClusterDestoryModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    @NotNull
    private AzureSparkCosmosCluster cluster;
    public CosmosSparkClusterDestoryCtrlProvider(
            @NotNull SettableControl<CosmosSparkClusterDestoryModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkCosmosCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<CosmosSparkClusterDestoryModel> validateAndDestroy(@NotNull String clusterName) {
        return Observable.just(new CosmosSparkClusterDestoryModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster name..."))
                .flatMap(toUpdate -> {
                    if (StringUtils.isEmpty(toUpdate.getClusterName())) {
                        return Observable.just(toUpdate.setErrorMessage("Error: Empty cluster name."));
                    }
                    if (!clusterName.equals(toUpdate.getClusterName())) {
                        return Observable.just(toUpdate.setErrorMessage("Error: Wrong cluster name."));
                    }

                    return cluster.destroy()
                            .map(cluster -> toUpdate.setErrorMessage(null))
                            .doOnNext(model -> {
                                // Send telemetry when delete cluster succeeded
                                AzureSparkCosmosClusterManager.getInstance().sendInfoTelemetry(
                                        TelemetryConstants.DELETE_A_CLUSTER, cluster.getGuid());
                            })
                            .onErrorReturn(err -> {
                                log().warn("Error delete a cluster. " + ExceptionUtils.getStackTrace(err));
                                if (err instanceof SparkAzureDataLakePoolServiceException) {
                                    String requestId = ((SparkAzureDataLakePoolServiceException) err).getRequestId();
                                    toUpdate.setRequestId(requestId);
                                    log().info("x-ms-request-id: " + requestId);
                                }
                                log().info("Cluster guid: " + cluster.getGuid());
                                // Send Telemetry when delete cluster failed
                                AzureSparkCosmosClusterManager.getInstance().sendErrorTelemetry(
                                        TelemetryConstants.DELETE_A_CLUSTER, err, cluster.getGuid());
                                return toUpdate.setErrorMessage(err.getMessage());
                            });
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(toUpdate -> StringUtils.isEmpty(toUpdate.getErrorMessage()));
    }
}
