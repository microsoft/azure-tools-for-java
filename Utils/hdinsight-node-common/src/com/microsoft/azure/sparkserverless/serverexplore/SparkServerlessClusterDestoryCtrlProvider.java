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
package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

public class SparkServerlessClusterDestoryCtrlProvider {
    @NotNull
    private SettableControl<SparkServerlessClusterDestoryModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    @NotNull
    private DestroyableCluster cluster;
    public SparkServerlessClusterDestoryCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterDestoryModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull DestroyableCluster cluster) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.cluster = cluster;
    }

    public Observable<SparkServerlessClusterDestoryModel> validateAndDestroy(@NotNull String clusterName) {
        return Observable.just(new SparkServerlessClusterDestoryModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster name..."))
                .map(toUpdate -> {
                    if (!clusterName.equals(toUpdate.getClusterName())) {
                        return toUpdate.setErrorMessage("Error: Wrong cluster name.");
                    }
                    try {
                        // destroy the cluster
                        cluster.destroy().toBlocking().single();
                    } catch (Exception e) {
                        return toUpdate.setErrorMessage("Delete failed: " + e.getMessage());
                    }
                    return toUpdate.setErrorMessage(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(toUpdate -> StringUtils.isEmpty(toUpdate.getErrorMessage()));
    }
}
