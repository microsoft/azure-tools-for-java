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

import com.intellij.core.JavaPsiBundle
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
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.util.PathUtil
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel
import com.microsoft.azure.hdinsight.spark.mock.SparkLocalRunner
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView
import com.microsoft.azure.hdinsight.spark.ui.SparkLocalRunParamsPanel
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Paths
import java.util.*

open class SparkBatchLocalRunState(val myProject: Project,
                                   val model: SparkLocalRunConfigurableModel,
                                   operation: Operation?,
                                   appInsightsMessage: String) :
    RunProfileStateWithAppInsightsEvent(UUID.randomUUID().toString(), appInsightsMessage, operation) {

    constructor(myProject: Project, model: SparkLocalRunConfigurableModel, operation: Operation?) :
            this(myProject, model, operation, HDInsightBundle.message("SparkRunConfigLocalRunButtonClick")!!)

    @Throws(ExecutionException::class)
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        // Spark Local Run/Debug
        val consoleView = SparkJobLogConsoleView(myProject)
        val processHandler = KillableColoredProcessHandler(createCommandlineForLocal(executor))

        return executor?.let {
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    val props = mapOf(
                        "IsSubmitSucceed" to "true",
                        "ExitCode" to event.exitCode.toString())
                    createAppInsightEvent(it, props)
                    EventUtil.logEventWithComplete(EventType.info, operation, getPostEventProperties(it, props), null)
                }
            })

            consoleView.attachToProcess(processHandler)

            DefaultExecutionResult(consoleView, processHandler)
        }
    }

    open fun getCommandLineVmParameters(executor: Executor?, params: JavaParameters, moduleName: String): List<String> {
        // Add jmockit as -javaagent
        val jmockitJarPath = params.classPath.pathList.stream()
                .filter { path -> path.toLowerCase().matches(".*\\Wjmockit-.*\\.jar".toRegex()) }
                .findFirst()
                .orElseThrow {
                    val exp = ExecutionException("Dependency jmockit hasn't been found in module `$moduleName` classpath")
                    createErrorEventWithComplete(executor, exp, ErrorType.userError, null)
                    exp
                }

        val javaAgentParam = "-javaagent:$jmockitJarPath"

        return listOf(javaAgentParam)
    }

    @Throws(ExecutionException::class)
    open fun createCommandlineForLocal(executor: Executor?): GeneralCommandLine {
        return createParams(executor).toCommandLine()
    }

    fun createParams(executor: Executor? = null,
                     hasClassPath: Boolean = true,
                     hasMainClass: Boolean = true,
                     hasJmockit: Boolean = true): JavaParameters {
        val params = JavaParameters()

        JavaParametersUtil.configureConfiguration(params, model)

        val mainModule = model.classpathModule?.let {
            ModuleManager.getInstance(myProject).findModuleByName(it)
        } ?: SparkLocalRun.defaultModule(myProject)

        if (mainModule != null) {
            params.configureByModule(mainModule, JavaParameters.JDK_AND_CLASSES_AND_TESTS)
        } else {
            JavaParametersUtil.configureProject(myProject, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null)
        }

        params.workingDirectory = Paths.get(model.dataRootDirectory, "__default__", "user", "current").toString()

        if (hasJmockit) {
            params.vmParametersList.addAll(
                    getCommandLineVmParameters(executor, params,
                            mainModule?.name ?: JavaPsiBundle.message("list.item.no.module")))
        }

        if (hasClassPath) {
            params.classPath.add(PathUtil.getJarPathForClass(SparkLocalRunner::class.java))
        }

        if (hasMainClass) {
            params.programParametersList
                    .addAt(0,
                            Optional.ofNullable(model.runClass)
                                    .filter { mainClass -> !mainClass.trim().isEmpty() }
                                    .orElseThrow {
                                        val exp = ExecutionException("Spark job's main class isn't set")
                                        createErrorEventWithComplete(executor, exp, ErrorType.userError, null)
                                        exp
                                    })
        }

        params.programParametersList
                .addAt(0, "--master local[" + (if (model.isIsParallelExecution) 2 else 1) + "]")

        if (SystemUtils.IS_OS_WINDOWS) {
            if (!Optional.ofNullable(params.env[SparkLocalRunParamsPanel.HADOOP_HOME_ENV])
                            .map { hadoopHome -> Paths.get(hadoopHome, "bin", SparkLocalRunParamsPanel.WINUTILS_EXE_NAME).toString() }
                            .map { File(it) }
                            .map { it.exists() }
                            .orElse(false)) {
                val exp = ExecutionException(
                        "winutils.exe should be in %HADOOP_HOME%\\bin\\ directory for Windows platform, please config it at 'Run/Debug Configuration -> Locally Run -> WINUTILS.exe location'.")
                createErrorEventWithComplete(executor, exp, ErrorType.userError, null)
                throw exp
            }
        }

        params.mainClass = SparkLocalRunner::class.java.canonicalName

        return params
    }
}