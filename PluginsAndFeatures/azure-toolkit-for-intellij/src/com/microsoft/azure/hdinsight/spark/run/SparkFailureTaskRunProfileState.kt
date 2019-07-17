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

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.util.JavaParametersUtil
import com.intellij.util.PathUtil
import com.microsoft.azure.hdinsight.spark.common.SparkFailureTaskDebugConfigurableModel
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle
import java.io.File
import java.util.*

open class SparkFailureTaskRunProfileState(val name: String,
                                           private val settingsConfigModel: SparkFailureTaskDebugConfigurableModel,
                                           operation: Operation?,
                                           appInsightsMessage: String)
        : RunProfileStateWithAppInsightsEvent(UUID.randomUUID().toString(), appInsightsMessage, operation) {

    constructor(name: String,
                settingsConfigModel: SparkFailureTaskDebugConfigurableModel,
                operation: Operation?)
            : this(name, settingsConfigModel, operation, HDInsightBundle.message("SparkRunConfigFailureTaskRunButtonClick"))

    val project = settingsConfigModel.project

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        doAppInsightOnExecute(executor)
        validate(executor)

        // Leverage Spark Local Run/Debug console view
        val consoleView = SparkJobLogConsoleView(project)
        val processHandler = KillableColoredProcessHandler(createCommandLine()).apply {
            addProcessListener(object: ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    val props = mapOf("ExitCode" to event.exitCode.toString())
                    EventUtil.logEventWithComplete(EventType.info, operation, getPostEventProperties(executor, props), null)
                }
            })
        }

        consoleView.attachToProcess(processHandler)

        return DefaultExecutionResult(consoleView, processHandler)
    }

    private val failureContextPath get() = settingsConfigModel.settings.failureContextPath

    protected open fun doAppInsightOnExecute(executor: Executor) {
        createAppInsightEvent(executor, null)
        EventUtil.logEvent(EventType.info, operation, getPostEventProperties(executor, null))
    }

    protected open val additionalVmParameters: Array<String>
        get() {
            // Failure Task Context file
            return arrayOf("-Dspark.failure.task.context=$failureContextPath")
        }

    // Validate the settings
    @Throws(ExecutionException::class)
    protected open fun validate(executor: Executor) {
        if (settingsConfigModel.settings.failureContextPath.isNullOrBlank()) {
            val exp =  ExecutionException("""
                Spark Failure Task Context file setting is blank, please fill it
                in `Run Configuration --> Spark Job Failure Context location`
                """.trimIndent())
            createErrorEventWithComplete(executor, exp, ErrorType.userError, getPostEventProperties(executor, null))
            throw exp
        }

        if (!File(settingsConfigModel.settings.failureContextPath).exists()) {
            val exp = ExecutionException("""
                The specified Spark Failure Task Context file '${settingsConfigModel.settings.failureContextPath}'
                doesn't exist, please select the right one in `Run Configuration --> Spark Job Failure Context location`
                """.trimIndent())
            createErrorEventWithComplete(executor, exp, ErrorType.userError, getPostEventProperties(executor, null))
            throw exp
        }
    }

    @Throws(ExecutionException::class)
    private fun createCommandLine() : GeneralCommandLine {
        val params = JavaParameters()

        JavaParametersUtil.configureConfiguration(params, settingsConfigModel)

        // The dependent spark-tools.jar is already in the Maven project lib/ directory
        JavaParametersUtil.configureProject(project, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null)

        // Additional VM parameters
        additionalVmParameters.forEach { params.vmParametersList.add(it) }

        // Put failure context runtime at beginning, after JDK6, the classpath support <dir>/*
        params.classPath.addFirst("${PathUtil.getCanonicalPath(settingsConfigModel.workingDirectory)}/runtime/*")

        // Prepare log4j.properties file
        settingsConfigModel.log4jProperties?.also { log4jProp ->
            val log4jPropertiesFile = File("${PathUtil.getCanonicalPath(settingsConfigModel.workingDirectory)}/conf/log4j.properties")
                    .apply {
                        parentFile.mkdir()
                        writeText(log4jProp)
                    }

            params.vmParametersList.add("-Dlog4j.configuration=$log4jPropertiesFile")
        }

        // Helper Main class
        params.mainClass = settingsConfigModel.runClass

        return params.toCommandLine()
    }
}