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

package com.microsoft.azure.cosmosspark.spark.console

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.project.Project
import com.microsoft.azure.cosmosspark.sdk.common.livy.interactive.CosmosSparkSession
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkItemGroupState
import com.microsoft.azure.hdinsight.spark.common.CosmosSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfiguration
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfigurationFactory
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import java.net.URI

class CosmosSparkScalaLivyConsoleRunConfiguration(project: Project,
                                                  configurationFactory: SparkScalaLivyConsoleRunConfigurationFactory,
                                                  batchRunConfiguration: LivySparkBatchJobRunConfiguration?,
                                                  name: String)
    : SparkScalaLivyConsoleRunConfiguration(
        project, configurationFactory, batchRunConfiguration, name)
{
    override val runConfigurationTypeName = "Azure Data Lake Spark Run Configuration"

    override fun createSession(sparkCluster: IClusterDetail): SparkSession {
        val livyUrl = ((sparkCluster as? AzureSparkCosmosCluster)?.livyUri?.toString()
                ?: throw RuntimeConfigurationError(
                        "Can't prepare Spark Cosmos interactive session since the Livy URI is null"))
                .trimEnd('/') + "/"

        return CosmosSparkSession(name, URI.create(livyUrl), sparkCluster.tenantId, sparkCluster.account)
    }

    override fun findCluster(clusterName: String): AzureSparkCosmosCluster {
        val adlAccount = (submitModel as? CosmosSparkSubmitModel
                ?: throw RuntimeConfigurationError(
                        "The submit model is not a CosmosSparkSubmitModel")).accountName
                ?: throw RuntimeConfigurationError("The target ADL account name is NULL")

        return AzureSparkCosmosClusterManager
                .getInstance()
                .getAccountByName(adlAccount)
                .clusters
                .filterIsInstance(AzureSparkCosmosCluster::class.java)
                .find { it.name == clusterName
                        && it.clusterStateForShow.equals(SparkItemGroupState.STABLE.toString(), ignoreCase = true )}
                ?: throw RuntimeConfigurationError(
                        "Can't find the workable(STABLE) target cluster $clusterName@$adlAccount")
    }
}
