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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster
import com.microsoft.azure.hdinsight.spark.common.CosmosSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel

class CosmosSparkSubmissionPanel(project: Project)
    : SparkSubmissionContentPanel(project, "Azure Data Lake Spark Pool") {

    override val clustersSelection: SparkClusterListRefreshableCombo by lazy { CosmosSparkClustersCombo().apply {
        Disposer.register(this@CosmosSparkSubmissionPanel, this@apply)
    }}

    override fun getData(data: SparkSubmitModel) {
        // Component -> Data
        super.getData(data)

        val serverlessData = data as? CosmosSparkSubmitModel ?: return
        val cluster = viewModel.clusterSelection.let {
            it.findClusterById(it.clusterListModelBehavior.value, it.toSelectClusterByIdBehavior.value) as? AzureSparkServerlessCluster
        }

        if (cluster != null) {
            serverlessData.tenantId = cluster.subscription.tenantId
            serverlessData.accountName = cluster.account.name
            serverlessData.clusterId = cluster.guid
            serverlessData.livyUri = cluster.livyUri?.toString() ?: ""
        }
    }
}
