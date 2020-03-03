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

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.microsoft.azure.arcadia.sdk.common.livy.interactive.ArcadiaSparkSession
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.console.SparkScalaConsoleBuilder
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfiguration
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunConfigurationFactory
import com.microsoft.azure.hdinsight.spark.console.SparkScalaLivyConsoleRunProfileState
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

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val sparkCluster = cluster as? ArcadiaSparkCompute ?: throw ExecutionException(RuntimeConfigurationError(
                "Can't prepare Synapse Spark interactive session since the spark pool cannot be found"))
        val livyUrl = sparkCluster.connectionUrl ?: throw ExecutionException(RuntimeConfigurationError(
                "Can't prepare Synapse Spark interactive session since Livy URL is empty"))

        val session = ArcadiaSparkSession(
                name, URI.create(livyUrl), sparkCluster.subscription.tenantId)
        return SparkScalaLivyConsoleRunProfileState(SparkScalaConsoleBuilder(project), session)
    }

    override fun checkRunnerSettings(runner: ProgramRunner<*>, runnerSettings: RunnerSettings?, configurationPerRunnerSettings: ConfigurationPerRunnerSettings?) {
        val arcadiaModel = (submitModel as? ArcadiaSparkSubmitModel)?.apply {
            if (sparkCompute == null || tenantId == null || sparkWorkspace == null) {
                log().warn("Synapse Spark pool is not selected. " +
                            "Spark pool: $sparkCompute, tenant id: $tenantId, spark workspace: $sparkWorkspace")
                throw RuntimeConfigurationError("Synapse Spark pool is not selected")
            }
        }
            ?: throw RuntimeConfigurationError("Can't cast submitModel to ArcadiaSparkSubmitModel")

        cluster = try {
            ArcadiaSparkComputeManager.getInstance()
                .findCompute(arcadiaModel.tenantId, arcadiaModel.sparkWorkspace, arcadiaModel.sparkCompute)
                .toBlocking()
                .first()
        } catch (ex: NoSuchElementException) {
            throw RuntimeConfigurationError(
                    "Can't find Synapse Spark pool (${arcadiaModel.sparkWorkspace}:${arcadiaModel.sparkCompute})"
                            + " at tenant ${arcadiaModel.tenantId}.")
        }
    }
}