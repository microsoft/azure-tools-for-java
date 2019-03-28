/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
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
import com.jetbrains.rider.run.configurations.IExecutorFactory
import com.jetbrains.rider.util.idea.getLogger
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfo
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider

class AzureFunctionsHostExecutorFactory(
        private val parameters: AzureFunctionsHostConfigurationParameters
) : IExecutorFactory {

    private val logger = getLogger<AzureFunctionsHostExecutorFactory>()

    override fun create(executorId: String, environment: ExecutionEnvironment): RunProfileState {
        val coreToolsInfo: FunctionsCoreToolsInfo? = FunctionsCoreToolsInfoProvider.retrieve()
                ?: throw CantRunException("Can't run Azure Functions host - path to core tools has not been configured.")

        val projectKind = parameters.projectKind
        logger.info("project kind is $projectKind")

        val dotNetExecutable = parameters.toDotNetExecutable()
        val runtimeToExecute = AzureFunctionsDotNetCoreRuntime(coreToolsInfo!!)
        logger.info("Configuration will be executed on ${runtimeToExecute.javaClass.name}")
        return when (executorId) {
            DefaultRunExecutor.EXECUTOR_ID -> runtimeToExecute.createRunState(dotNetExecutable, environment)
            DefaultDebugExecutor.EXECUTOR_ID -> runtimeToExecute.createDebugState(dotNetExecutable, environment)
            else -> throw CantRunException("Unsupported executor $executorId")
        }
    }
}