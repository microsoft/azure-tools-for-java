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
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.microsoft.azure.hdinsight.common.classifiedexception.ClassifiedExceptionFactory
import com.microsoft.azure.hdinsight.common.classifiedexception.*
import com.microsoft.azure.hdinsight.common.print
import com.microsoft.azure.hdinsight.spark.common.ISparkBatchJob
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azuretools.telemetry.TelemetryProperties
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle
import java.util.*

open class SparkBatchRemoteRunState(private val sparkSubmitModel: SparkSubmitModel,
                                    operation: Operation?,
                                    override var sparkBatch: ISparkBatchJob)
    : RunProfileStateWithAppInsightsEvent(UUID.randomUUID().toString(),
                                          HDInsightBundle.message("SparkRunConfigRunButtonClick")!!,
                                          operation),
      SparkBatchRemoteRunProfileState,
      TelemetryProperties {
    override var remoteProcessCtrlLogHandler: SparkBatchJobProcessCtrlLogOut? = null
    override var executionResult: ExecutionResult? = null
    override var consoleView: ConsoleView? = null
    private var isStopButtonClicked: Boolean = false
        get() = remoteProcessCtrlLogHandler?.getUserData(ProcessHandler.TERMINATION_REQUESTED) == true
    private var isDisconnectButtonClicked: Boolean = false
    private var isArtifactUploaded: Boolean = false
    private var isSubmitSucceed: Boolean = false
    private var isJobKilled: Boolean = false
    private var isJobRunSucceed: Boolean? = null
    private var jobState: String? = null
    private var diagnostics: String? = null

    override fun execute(executor: Executor?, programRunner: ProgramRunner<*>): ExecutionResult? {
        val sparkConsoleView = consoleView
        if (remoteProcessCtrlLogHandler == null || executionResult == null || sparkConsoleView == null) {
            throw ExecutionException("Spark Batch Job execution result is not ready")
        }

        return executor?.let {
            remoteProcessCtrlLogHandler!!.getEventSubject()
                    .subscribe({
                        when (it) {
                            is SparkBatchJobArtifactUploadedEvent -> this.isArtifactUploaded = true
                            is SparkBatchJobSubmittedEvent -> this.isSubmitSucceed = true
                            is SparkBatchJobKilledEvent -> this.isJobKilled = true
                            is SparkBatchJobFinishedEvent -> {
                                this.isJobRunSucceed = it.isJobSucceed
                                this.jobState = it.state
                                this.diagnostics = it.diagnostics
                            }
                            is SparkBatchJobDisconnectEvent -> this.isDisconnectButtonClicked = true
                            else -> {
                            }
                        }
                    }, {})
            remoteProcessCtrlLogHandler!!.getCtrlSubject().subscribe(
                    { messageWithType -> sparkConsoleView.print(messageWithType) },
                    { err ->
                        val classifiedEx = ClassifiedExceptionFactory.createClassifiedException(err)
                        classifiedEx.logStackTrace()

                        createAppInsightEvent(it, toProperties())
                        createErrorEventWithComplete(it, classifiedEx, classifiedEx.errorType, toProperties())

                        val errMessage = classifiedEx.message
                        consoleView!!.print("ERROR: $errMessage", ConsoleViewContentType.ERROR_OUTPUT)
                        classifiedEx.handleByUser()
                    },
                    { onComplete(it) })

            executionResult
        }
    }

    override fun getSubmitModel(): SparkSubmitModel {
        return sparkSubmitModel
    }

    open fun onComplete(executor: Executor) {
        createAppInsightEvent(executor, toProperties())
        createInfoEventWithComplete(executor, toProperties())
    }

    override fun toProperties(): MutableMap<String, String> {
        return mutableMapOf(
                "isArtifactUploaded" to isArtifactUploaded.toString(),
                "isJobSubmitSucceed" to isSubmitSucceed.toString(),
                "isJobKilled" to isJobKilled.toString(),
                "isJobRunSucceed" to if (!isSubmitSucceed || isJobKilled) "false" else (isJobRunSucceed?.toString() ?: "unknown"),
                "livyState" to (jobState ?: "unknown"),
                "livyDiagnostics" to (diagnostics ?: "null"),
                "isDisconnectButtonClicked" to isDisconnectButtonClicked.toString(),
                "isStopButtonClicked" to isStopButtonClicked.toString())
    }
}