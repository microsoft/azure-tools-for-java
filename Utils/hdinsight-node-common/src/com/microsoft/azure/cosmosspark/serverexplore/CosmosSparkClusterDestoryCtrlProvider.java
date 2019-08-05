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
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
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
                            .onErrorReturn(err -> {
                                log().warn("Error provision a cluster. " + ExceptionUtils.getStackTrace(err));
                                if (err instanceof SparkAzureDataLakePoolServiceException) {
                                    String requestId = ((SparkAzureDataLakePoolServiceException) err).getRequestId();
                                    toUpdate.setRequestId(requestId);
                                    log().info("x-ms-request-id: " + requestId);
                                }
                                log().info("Cluster guid: " + cluster.getGuid());
                                return toUpdate.setErrorMessage(err.getMessage());
                            });
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(toUpdate -> StringUtils.isEmpty(toUpdate.getErrorMessage()));
    }
}
