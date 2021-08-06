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

package com.microsoft.azure.projectarcadia.spark.console

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.project.Project
import com.microsoft.azure.arcadia.sdk.common.livy.interactive.ArcadiaSparkSession
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfiguration
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfigurationFactory
import com.microsoft.azure.hdinsight.spark.run.configuration.ArcadiaSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager
import java.net.URI
import java.util.*

class ArcadiaSparkScalaLivyConsoleRunConfiguration(project: Project,
                                                   configurationFactory: SparkScalaLivyConsoleRunConfigurationFactory,
                                                   batchRunConfiguration: LivySparkBatchJobRunConfiguration?,
                                                   name: String)
    : SparkScalaLivyConsoleRunConfiguration(
        project, configurationFactory, batchRunConfiguration, name), ILogger {
    override val runConfigurationTypeName: String = "Synapse Spark Run Configuration"

    override fun createSession(sparkCluster: IClusterDetail): SparkSession {
        val livyUrl = sparkCluster.connectionUrl ?: throw RuntimeConfigurationError(
                "Can't prepare Apache Spark for Azure Synapse interactive session since Livy URL is empty")

        return ArcadiaSparkSession(name, URI.create(livyUrl), sparkCluster.subscription.tenantId)
    }

    override fun findCluster(clusterName: String): ArcadiaSparkCompute {
        val arcadiaModel = (submitModel as? ArcadiaSparkSubmitModel)?.apply {
            if (sparkCompute == null || tenantId == null || sparkWorkspace == null) {
                log().warn("Apache Spark Pool for Azure Synapse is not selected. " +
                        "Spark pool: $sparkCompute, tenant id: $tenantId, spark workspace: $sparkWorkspace")
                throw RuntimeConfigurationError("Apache Spark Pool for Azure Synapse is not selected")
            }
        } ?: throw RuntimeConfigurationError("Can't cast submitModel to ArcadiaSparkSubmitModel")

        return try {
            ArcadiaSparkComputeManager.getInstance()
                    .findCompute(arcadiaModel.tenantId, arcadiaModel.sparkWorkspace, arcadiaModel.sparkCompute)
                    .toBlocking()
                    .first()
        } catch (ex: NoSuchElementException) {
            throw RuntimeConfigurationError(
                    "Can't find Apache Spark Pool for Azure Synapse (${arcadiaModel.sparkWorkspace}:${arcadiaModel.sparkCompute})"
                            + " at tenant ${arcadiaModel.tenantId}.")
        }
    }
}