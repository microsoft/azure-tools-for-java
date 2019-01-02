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
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.CreateSparkBatchJobParameters
import com.microsoft.azure.hdinsight.spark.common.*
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkConfiguration
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observer
import java.io.IOException
import java.util.*

class CosmosServerlessSparkBatchRunner : SparkBatchJobRunner() {
    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return SparkBatchJobRunExecutor.EXECUTOR_ID == executorId && profile.javaClass == CosmosServerlessSparkConfiguration::class.java
    }

    override fun getRunnerId(): String {
        return "CosmosServerlessSparkBatchRun"
    }

    @Throws(ExecutionException::class)
    override fun buildSparkBatchJob(submitModel: SparkSubmitModel, ctrlSubject: Observer<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>>): ISparkBatchJob {
        val submissionParameter = submitModel.submissionParameter as CreateSparkBatchJobParameters
        // FIXME: Set job name to main class name?
        submissionParameter.name = submissionParameter.mainClassName
        val adlAccountName = submissionParameter.clusterName
        val account = AzureSparkCosmosClusterManager.getInstance().getAccountByName(adlAccountName)
                ?: throw ExecutionException("Can't find ADLA account '$adlAccountName'")

        val accessToken = try {
            account.http.accessToken
        } catch (ex: IOException) {
            log().warn("Error getting access token. " + ExceptionUtils.getStackTrace(ex))
            throw ExecutionException("Error getting access token.", ex)
        }
        val storageRootPath = account.storageRootPath ?: throw ExecutionException("Error getting ADLS storage root path for account ${account.name}")

        return CosmosServerlessSparkBatchJob(account, AdlsDeploy(storageRootPath, accessToken),submissionParameter, SparkBatchSubmission.getInstance(), ctrlSubject)
    }
}