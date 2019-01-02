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
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.BrowserUtil
import com.microsoft.azure.hdinsight.common.HDInsightUtil
import com.microsoft.azure.hdinsight.common.MessageInfoType
import com.microsoft.azure.hdinsight.common.classifiedexception.*
import com.microsoft.azure.hdinsight.spark.common.CosmosSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException
import com.microsoft.azure.hdinsight.spark.run.configuration.ArisSparkSubmitModel
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkSubmitModel
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle
import java.net.URI
import java.util.*

open class SparkBatchRemoteRunState(private val sparkSubmitModel: SparkSubmitModel)
    : RunProfileStateWithAppInsightsEvent, SparkBatchRemoteRunProfileState  {
    override var remoteProcessCtrlLogHandler: SparkBatchJobProcessCtrlLogOut? = null
    override var executionResult: ExecutionResult? = null
    override var consoleView: ConsoleView? = null
    override val uuid = UUID.randomUUID().toString()
    override val appInsightsMessage = HDInsightBundle.message("SparkRunConfigRunButtonClick")!!

    override fun execute(executor: Executor?, programRunner: ProgramRunner<*>): ExecutionResult? {
        if (remoteProcessCtrlLogHandler == null || executionResult == null || consoleView == null) {
            throw ExecutionException("Spark Batch Job execution result is not ready")
        }

        return executor?.let {
            remoteProcessCtrlLogHandler!!.getCtrlSubject().subscribe(
                    { messageWithType ->
                        // Redirect the remote process control message to console view
                        when (messageWithType.key) {
                            MessageInfoType.Info ->
                                consoleView!!.print("INFO: ${messageWithType.value}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                            MessageInfoType.Warning, MessageInfoType.Log ->
                                consoleView!!.print("LOG: ${messageWithType.value}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                            MessageInfoType.Hyperlink ->
                                BrowserUtil.browse(URI.create(messageWithType.value))
                            else ->
                            {
                                consoleView!!.print("ERROR: ${messageWithType.value}\n", ConsoleViewContentType.ERROR_OUTPUT)

                                val classifiedEx = ClassifiedExceptionFactory
                                        .createClassifiedException(YarnDiagnosticsException(messageWithType.value))
                                classifiedEx.logStackTrace()
                                classifiedEx.handleByUser()
                            }
                        }
                    },
                    { err ->
                        val classifiedEx = ClassifiedExceptionFactory.createClassifiedException(err)
                        classifiedEx.logStackTrace()

                        val errMessage = classifiedEx.message
                        createAppInsightEvent(it, mapOf(
                                "IsSubmitSucceed" to "false",
                                "SubmitFailedReason" to HDInsightUtil.normalizeTelemetryMessage(errMessage)))

                        consoleView!!.print("ERROR: $errMessage", ConsoleViewContentType.ERROR_OUTPUT)
                        classifiedEx.handleByUser()
                    },
                    { onSuccess(it) })

            programRunner.onProcessStarted(null, executionResult)

            executionResult
        }
    }

    @Throws(ExecutionException::class)
    override fun checkSubmissionParameter() {
        val parameter = getSubmitModel().submissionParameter

        if (parameter.clusterName.isNullOrBlank()) {
            throw ExecutionException("The ${getSubmitModel().sparkClusterTypeDisplayName} to submit job is not selected, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.")
        }

        if (parameter.artifactName.isNullOrBlank() && parameter.localArtifactPath.isNullOrBlank()) {
            throw ExecutionException("The artifact to submit is not selected, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.")
        }

        if (parameter.mainClassName.isNullOrBlank()) {
            throw ExecutionException("The main class name is empty, please config it at 'Run/Debug configuration -> Remotely Run in Cluster'.")
        }
    }

    override fun getSubmitModel(): SparkSubmitModel {
        return sparkSubmitModel
    }

    open fun onSuccess(executor: Executor) {
        createAppInsightEvent(executor, mapOf("IsSubmitSucceed" to "true"))
    }
}