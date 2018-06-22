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

package com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode;

import com.microsoft.azure.hdinsight.sdk.cluster.DestroyableCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import rx.subjects.PublishSubject;

public class SparkServerlessClusterOps {
    private static SparkServerlessClusterOps instance = new SparkServerlessClusterOps();

    @NotNull
    private final PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, SparkServerlessClusterNode>> destroyAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessAccount, SparkServerlessADLAccountNode>> provisionAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> monitorAction;
    @NotNull
    private final PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> updateAction;

    private SparkServerlessClusterOps() {
        destroyAction = PublishSubject.create();
        provisionAction = PublishSubject.create();
        monitorAction = PublishSubject.create();
        updateAction = PublishSubject.create();
    }

    @NotNull
    public static SparkServerlessClusterOps getInstance() {
        return instance;
    }

    @NotNull
    public PublishSubject<Triple<
            AzureSparkServerlessAccount, DestroyableCluster, SparkServerlessClusterNode>> getDestroyAction() {
        return destroyAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkServerlessAccount, SparkServerlessADLAccountNode>> getProvisionAction() {
        return provisionAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> getMonitorAction() {
        return monitorAction;
    }

    @NotNull
    public PublishSubject<Pair<AzureSparkServerlessCluster, SparkServerlessClusterNode>> getUpdateAction() {
        return updateAction;
    }
}
