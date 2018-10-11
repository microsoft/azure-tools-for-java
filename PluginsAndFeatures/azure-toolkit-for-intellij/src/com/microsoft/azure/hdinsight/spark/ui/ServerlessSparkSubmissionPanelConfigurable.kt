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

package com.microsoft.azure.hdinsight.spark.ui

import com.google.common.collect.ImmutableSortedSet
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessClusterManager
import com.microsoft.azure.hdinsight.spark.common.ServerlessSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import org.apache.commons.lang3.StringUtils
import rx.Observable
import java.util.stream.Collectors

class ServerlessSparkSubmissionPanelConfigurable(project: Project, submissionPanel: SparkSubmissionContentPanel)
    : SparkSubmissionContentPanelConfigurable(project, submissionPanel), ILogger {

    init {
        jobUploadStorageCtrl = object : SparkSubmissionJobUploadStorageCtrl(storageWithUploadPathPanel) {
            override fun getClusterName(): String? = selectedClusterDetail?.name

            override fun getClusterDetail(): IClusterDetail? {
                if (StringUtils.isEmpty(getClusterName())) {
                    return null
                }
                return AzureSparkServerlessClusterManager.getInstance().clusters.stream()
                        .filter { clusterDetail -> clusterDetail.name == getClusterName() }
                        .collect(Collectors.toList())
                        .getOrNull(0)
            }
        }

    }

    override fun getType(): String = "Azure Data Lake Spark Pool"

    override fun getClusterDetails(): ImmutableSortedSet<out IClusterDetail> {
        return AzureSparkServerlessClusterManager.getInstance().clusters
    }

    override fun getClusterDetailsWithRefresh(): Observable<ImmutableSortedSet<out IClusterDetail>> {
        return AzureSparkServerlessClusterManager.getInstance().fetchClusters().map { it.clusters }
    }

    override fun getData(data: SparkSubmitModel?) {
        // Component -> Data
        val serverlessData = data as ServerlessSparkSubmitModel
        val cluster = selectedClusterDetail as? AzureSparkServerlessCluster

        if (cluster != null) {
            serverlessData.tenantId = cluster.subscription.tenantId
            serverlessData.accountName = cluster.account.name
            serverlessData.clusterId = cluster.guid
            serverlessData.livyUri = cluster.livyUri?.toString() ?: ""
        }

        super.getData(data)
    }
}