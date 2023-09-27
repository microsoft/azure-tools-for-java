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
import com.intellij.execution.filters.BrowserHyperlinkInfo
import com.intellij.execution.filters.Filter
import com.intellij.execution.ui.ConsoleView
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.sdk.cluster.InternalUrlMapping
import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.ArisSparkConfiguration
import com.microsoft.azure.sqlbigdata.spark.common.ArisSparkBatchJob
import rx.Observable
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class ArisSparkBatchRunner : SparkBatchJobRunner() {

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return SparkBatchJobRunExecutor.EXECUTOR_ID == executorId && profile.javaClass == ArisSparkConfiguration::class.java
    }

    override fun getRunnerId(): String {
        return "ArisSparkBatchRun"
    }

    override fun addConsoleViewFilter(job: ISparkBatchJob, consoleView: ConsoleView) {
        (job as? SparkBatchJob)?.let {
            val mapping = it.cluster as? InternalUrlMapping
            if (mapping != null) {
                consoleView.addMessageFilter { line, entireLength ->
                    val matcher = Pattern.compile("""http[s]?://[^\s]+""", Pattern.CASE_INSENSITIVE).matcher(line)
                    val items = mutableListOf<Filter.ResultItem>()
                    val textStartOffset = entireLength - line.length
                    while (matcher.find()) {
                        val mappedUrl = mapping.mapInternalUrlToPublic(matcher.group(0))
                        items.add(
                            Filter.ResultItem(
                                textStartOffset + matcher.start(), textStartOffset + matcher.end(),
                                BrowserHyperlinkInfo(mappedUrl)
                            )
                        )
                    }
                    if (items.size != 0) Filter.Result(items) else null
                }
            }
        }
    }

    override fun buildSparkBatchJob(submitModel: SparkSubmitModel): Observable<ISparkBatchJob> =
        Observable.fromCallable {
            val clusterName = submitModel.submissionParameter.clusterName
            val clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName)
                .orElseThrow { ExecutionException("Can't find cluster named $clusterName") }

            val jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(submitModel, clusterDetail)
            // UTC Time sample: 2019-07-09T02:47:34.245Z
            val currentUtcTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

            ArisSparkBatchJob(
                clusterDetail,
                // In the latest 0.6.0-incubating livy, livy prevents user from creating sessions that have the same session name
                // Livy release notes: https://livy.apache.org/history/
                // JIRA: https://issues.apache.org/jira/browse/LIVY-41
                submitModel.submissionParameter.apply {
                    updateStorageConfigForSubmissionParameter(submitModel).apply { name = mainClassName + "_$currentUtcTime" }
                },
                SparkBatchSubmission.getInstance(),
                jobDeploy
            )
        }
}