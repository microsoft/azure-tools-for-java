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

package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManagerEx
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.hdinsight.common.CommonConst
import com.microsoft.azure.hdinsight.common.StreamUtil
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobRunExecutor.EXECUTOR_ID
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azuretools.telemetry.TelemetryConstants

class SparkJobRunAction
    : SparkRunConfigurationAction(
        "SparkJobRun",
        "Submit Apache Spark Application to remote cluster",
        StreamUtil.getImageResourceFile(CommonConst.ToolWindowSparkJobRunIcon_13x_Path)?: AllIcons.Actions.Upload) {

    override val runExecutor: Executor
        get() = ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID)
            ?: throw RuntimeException("Can't find executor $EXECUTOR_ID from executor registry")

    override fun getServiceName(event: AnActionEvent): String {
        val project = event.project ?: return super.getServiceName(event)
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration
        return (selectedConfigSettings?.configuration as LivySparkBatchJobRunConfiguration).sparkApplicationType.value
    }

    override fun getOperationName(event: AnActionEvent?): String {
        return TelemetryConstants.RUN_REMOTE_SPARK_JOB
    }
}