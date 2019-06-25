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

package com.microsoft.azure.hdinsight.spark.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.microsoft.azure.hdinsight.common.MessageInfoType
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager
import com.microsoft.azure.hdinsight.spark.common.*
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkRunConfiguration
import rx.Observer
import java.net.URI
import java.util.AbstractMap.SimpleImmutableEntry

class CosmosSparkBatchRunner : SparkBatchJobRunner() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return SparkBatchJobRunExecutor.EXECUTOR_ID == executorId && profile.javaClass == CosmosSparkRunConfiguration::class.java
    }

    override fun getRunnerId(): String {
        return "CosmosSparkBatchRun"
    }

    @Throws(ExecutionException::class)
    override fun buildSparkBatchJob(submitModel: SparkSubmitModel, ctrlSubject: Observer<SimpleImmutableEntry<MessageInfoType, String>>): ISparkBatchJob {
        val tenantId = (submitModel as CosmosSparkSubmitModel).tenantId
        val accountName = submitModel.accountName

        if (submitModel.clusterId == null) {
            throw ExecutionException("Can't get the Azure Serverless Spark cluster, please sign in and refresh.")
        }

        val clusterId = submitModel.clusterId
        try {
            val clusterDetail = AzureSparkCosmosClusterManager.getInstance().findCluster(accountName, clusterId)
                .toBlocking().singleOrDefault(null) ?: throw Exception("Can't get Spark on Cosmos cluster")

            val livyUri = submitModel.livyUri?.let { URI.create(it) }
                ?: clusterDetail.get().toBlocking().singleOrDefault(clusterDetail).livyUri

             this.sparkBatchJob = CosmosSparkBatchJob(
                    submitModel.submissionParameter,
                    SparkBatchAzureSubmission(tenantId, accountName, clusterId, livyUri),
                    ctrlSubject)
            return this.sparkBatchJob as CosmosSparkBatchJob
        } catch (e: Exception) {
            throw ExecutionException("Can't get Spark on Cosmos cluster, please sign in and refresh.", e)
        }

    }
}