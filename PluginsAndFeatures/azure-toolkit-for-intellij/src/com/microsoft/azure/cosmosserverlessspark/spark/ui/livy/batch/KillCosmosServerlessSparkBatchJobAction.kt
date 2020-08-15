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

package com.microsoft.azure.cosmosserverlessspark.spark.ui.livy.batch

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SchedulerState
import com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models.SparkBatchJob
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.util.PluginUtil
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import java.io.IOException

class KillCosmosServerlessSparkBatchJobAction(private val account: AzureSparkServerlessAccount,
                                              private val job: SparkBatchJob) : AzureAnAction(AllIcons.Actions.Cancel), ILogger {
    override fun onActionPerformed(anActionEvent: AnActionEvent, operation: Operation?): Boolean {
        try {
            if (job.id() == null) {
                val errorMsg = "Failed to kill spark job ${job.name()}. Batch job id is empty"
                log().warn(errorMsg)
                PluginUtil.displayErrorDialog("Kill Serverless Spark Job", errorMsg)
                EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.systemError, IOException(errorMsg), null, null)
            } else {
                account.getSparkBatchJobRequest(job.id().toString())
                        .flatMap { respBatchJob ->
                            if (respBatchJob.state() == SchedulerState.ENDED || respBatchJob.state() == SchedulerState.FINALIZING) {
                                val errMsg = "Can't kill spark job ${respBatchJob.name()}. It's in '${respBatchJob.schedulerState()}' state!"
                                PluginUtil.displayInfoDialog("Kill Serverless Spark Job", errMsg)
                                EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.userError, IOException(errMsg), null, null)
                                return@flatMap Observable.just(respBatchJob)
                            } else {
                                return@flatMap account.killSparkBatchJobRequest(job.id().toString())
                                        .doOnNext { resp ->
                                            if (resp.code >= 300) {
                                                val errMsg = "Failed to kill spark job ${respBatchJob.name()}. ${resp.message}"
                                                PluginUtil.displayErrorDialog("Kill Serverless Spark Job", errMsg)
                                                EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.serviceError, IOException(errMsg), null, null)
                                            } else {
                                                PluginUtil.displayInfoDialog("Kill Serverless Spark Job", "Successfully killed Spark Job ${respBatchJob.name()}!")
                                                EventUtil.logEventWithComplete(EventType.info, operation, null, null)
                                            }
                                        }
                                        .map { respBatchJob }
                            }
                        }
                        .subscribe(
                                {},
                                { err ->
                                    val errMsg = "Failed to kill spark job ${job.name()}. ${err.message}"
                                    PluginUtil.displayErrorDialog("Kill Serverless Spark Job", errMsg)
                                    log().warn("Failed to kill serverless spark job. " + ExceptionUtils.getStackTrace(err))
                                    EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.serviceError, IOException(errMsg), null, null)
                                }
                        )
            }
            return false
        } catch (ignored: RuntimeException) {
            return true
        }
    }
}