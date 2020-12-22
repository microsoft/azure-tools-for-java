/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.microsoft.azure.hdinsight.spark.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.microsoft.azure.hdinsight.common.WasbUri
import com.microsoft.azure.hdinsight.spark.common.*
import com.microsoft.azure.hdinsight.spark.run.configuration.ArcadiaSparkConfiguration
import com.microsoft.azure.hdinsight.spark.run.configuration.ArcadiaSparkSubmitModel
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import rx.Observable
import java.net.URI

class ArcadiaSparkBatchRunner : SparkBatchJobRunner() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return SparkBatchJobRunExecutor.EXECUTOR_ID == executorId && profile.javaClass == ArcadiaSparkConfiguration::class.java
    }

    override fun getRunnerId(): String {
        return "ArcadiaSparkBatchRun"
    }

    val secureStore: SecureStore? = ServiceManager.getServiceProvider(SecureStore::class.java)

    override fun buildSparkBatchJob(submitModel: SparkSubmitModel)
            : Observable<ISparkBatchJob> = Observable.fromCallable {
        (submitModel as ArcadiaSparkSubmitModel).apply {
            if (sparkCompute == null || tenantId == null || sparkWorkspace == null) {
                log().warn("Apache Spark Pool for Azure Synapse is not selected. " +
                        "spark pool: $sparkCompute, tenant id: $tenantId, spark workspace: $sparkWorkspace")
                throw ExecutionException("Apache Spark Pool for Azure Synapse is not selected")
            }
        }
    }.flatMap { arcadiaModel -> ArcadiaSparkComputeManager.getInstance()
            .findCompute(arcadiaModel.tenantId, arcadiaModel.sparkWorkspace, arcadiaModel.sparkCompute)
            .first()
            .map { compute ->
                val submission = SparkBatchArcadiaSubmission(
                        arcadiaModel.tenantId,
                        arcadiaModel.sparkWorkspace,
                        URI.create(arcadiaModel.livyUri),
                        arcadiaModel.jobName,
                        compute.workSpace.webUrl
                )

                val jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(submitModel, compute)

                ArcadiaSparkBatchJob(
                        updateStorageConfigForSubmissionParameter(submitModel),
                        submission,
                        jobDeploy)
            }}
}