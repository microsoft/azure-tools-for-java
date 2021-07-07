/**
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.rider.run.configurations.IExecutorFactory
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfo
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import org.jetbrains.plugins.azure.functions.run.localsettings.FunctionLocalSettingsUtil
import org.jetbrains.plugins.azure.functions.run.localsettings.FunctionsWorkerRuntime
import java.io.File

class AzureFunctionsHostExecutorFactory(
        private val parameters: AzureFunctionsHostConfigurationParameters
) : IExecutorFactory {

    companion object {
        private val logger = Logger.getInstance(AzureFunctionsHostExecutorFactory::class.java)
    }

    override fun create(executorId: String, environment: ExecutionEnvironment): RunProfileState {
        val coreToolsInfo: FunctionsCoreToolsInfo? = FunctionsCoreToolsInfoProvider.retrieve()
                ?: throw CantRunException("Can't run Azure Functions host - path to core tools has not been configured.")

        val projectKind = parameters.projectKind
        logger.info("Project kind is $projectKind")

        logger.debug("Patching host.json file to reflect run configuration parameters")
        HostJsonPatcher.tryPatchHostJsonFile(parameters.workingDirectory, parameters.functionNames)

        logger.debug("Determine worker runtime from local.settings.json")
        val functionLocalSettings = FunctionLocalSettingsUtil.readFunctionLocalSettings(
                project = parameters.project,
                basePath = File(parameters.projectFilePath).parent)
        val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionsWorkerRuntime.DotNetDefault
        logger.info("Worker runtime: $workerRuntime")

        val dotNetExecutable = parameters.toDotNetExecutable()
        val runtimeToExecute = AzureFunctionsDotNetCoreRuntime(coreToolsInfo!!, workerRuntime)
        logger.info("Configuration will be executed on ${runtimeToExecute.javaClass.name}")
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }
}