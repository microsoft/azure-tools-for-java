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
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.util.PathUtil
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel
import com.microsoft.azure.hdinsight.spark.mock.SparkLocalRunner
import com.microsoft.azure.hdinsight.spark.ui.SparkJobLogConsoleView
import com.microsoft.azure.hdinsight.spark.ui.SparkLocalRunConfigurable
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.file.Paths
import java.util.*

open class SparkBatchLocalRunState(val myProject: Project, val model: SparkLocalRunConfigurableModel)
    : RunProfileStateWithAppInsightsEvent {
    override val uuid = UUID.randomUUID().toString()
    override val appInsightsMessage = HDInsightBundle.message("SparkRunConfigLocalRunButtonClick")!!

    @Throws(ExecutionException::class)
    override fun execute(executor: Executor?, runner: ProgramRunner<*>): ExecutionResult? {
        // Spark Local Run/Debug
        val consoleView = SparkJobLogConsoleView(myProject)
        val processHandler = KillableColoredProcessHandler(createCommandlineForLocal())

        return executor?.let {
            processHandler.addProcessListener(object : ProcessAdapter() {
                override fun processTerminated(event: ProcessEvent) {
                    createAppInsightEvent(it, mapOf(
                            "IsSubmitSucceed" to "true",
                            "ExitCode" to event.exitCode.toString()))
                }
            })

            consoleView.attachToProcess(processHandler)

            DefaultExecutionResult(consoleView, processHandler)
        }
    }

    open fun getCommandLineVmParameters(params: JavaParameters, moduleName: String): List<String> {
        // Add jmockit as -javaagent
        val jmockitJarPath = params.classPath.pathList.stream()
                .filter { path -> path.toLowerCase().matches(".*\\Wjmockit-.*\\.jar".toRegex()) }
                .findFirst()
                .orElseThrow { ExecutionException("Dependency jmockit hasn't been found in module `$moduleName` classpath") }

        val javaAgentParam = "-javaagent:$jmockitJarPath"

        return listOf(javaAgentParam)
    }

    @Throws(ExecutionException::class)
    open fun createCommandlineForLocal(): GeneralCommandLine {
        return createParams().toCommandLine()
    }

    fun createParams(hasClassPath: Boolean = true,
                     hasMainClass: Boolean = true,
                     hasJmockit: Boolean = true): JavaParameters {
        val params = JavaParameters()

        JavaParametersUtil.configureConfiguration(params, model)

        val mainModule = model.classpathModule?.let {
            ModuleManager.getInstance(myProject).findModuleByName(it)
        } ?: ModuleManager.getInstance(myProject).modules.first { it.name.equals(myProject.name, ignoreCase = true) }

        if (mainModule != null) {
            params.configureByModule(mainModule, JavaParameters.JDK_AND_CLASSES_AND_TESTS)
        } else {
            JavaParametersUtil.configureProject(myProject, params, JavaParameters.JDK_AND_CLASSES_AND_TESTS, null)
        }

        params.workingDirectory = Paths.get(model.dataRootDirectory, "__default__", "user", "current").toString()

        if (hasJmockit) {
            params.vmParametersList.addAll(getCommandLineVmParameters(params, mainModule.name))
        }

        if (hasClassPath) {
            params.classPath.add(PathUtil.getJarPathForClass(SparkLocalRunner::class.java))
        }

        if (hasMainClass) {
            params.programParametersList
                    .addAt(0,
                            Optional.ofNullable(model.runClass)
                                    .filter { mainClass -> !mainClass.trim().isEmpty() }
                                    .orElseThrow { ExecutionException("Spark job's main class isn't set") })
        }

        params.programParametersList
                .addAt(0, "--master local[" + (if (model.isIsParallelExecution) 2 else 1) + "]")

        if (SystemUtils.IS_OS_WINDOWS) {
            if (!Optional.ofNullable(params.env[SparkLocalRunConfigurable.HADOOP_HOME_ENV])
                            .map { hadoopHome -> Paths.get(hadoopHome, "bin", SparkLocalRunConfigurable.WINUTILS_EXE_NAME).toString() }
                            .map { File(it) }
                            .map { it.exists() }
                            .orElse(false)) {
                throw ExecutionException(
                        "winutils.exe should be in %HADOOP_HOME%\\bin\\ directory for Windows platform, please config it at 'Run/Debug Configuration -> Locally Run -> WINUTILS.exe location'.")
            }
        }

        params.mainClass = SparkLocalRunner::class.java.canonicalName

        return params
    }
}