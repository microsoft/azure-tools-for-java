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

package com.microsoft.azure.hdinsight.spark.service

import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail
import rx.Observable
import rx.Observable.fromCallable

object SparkClustersServices {
    val arisSparkClustersRefreshed: Observable<List<IClusterDetail>> = fromCallable {
        ClusterManagerEx.getInstance().clusterDetails.asSequence()
                .filter { it is SqlBigDataLivyLinkClusterDetail }
                .sortedBy { it.title }
                .toList()
    }.share()

    val cosmosSparkClustersRefreshed: Observable<List<IClusterDetail>> =
            AzureSparkCosmosClusterManager.getInstance().fetchClusters()
                    .map { it.clusters.asIterable().toList() }
                    .share()

    val cosmosServerlessSparkAccountsRefreshed: Observable<out List<IClusterDetail>> =
            AzureSparkCosmosClusterManager.getInstance().fetchClusters()
                    .map { it.accounts.asIterable().toList() }
                    .share()

    val hdinsightSparkClustersRefreshed: Observable<List<IClusterDetail>> = fromCallable {
        ClusterManagerEx.getInstance().clusterDetails.asSequence()
                .filter { ClusterManagerEx.getInstance().hdInsightClusterFilterPredicate.test(it) }
                .sortedBy { it.title }
                .toList()
    }.share()

}
