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

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rider.run.DotNetProcessRunProfileState
import com.jetbrains.rider.run.IDotNetDebugProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RunningAssemblyInfo
import com.jetbrains.rider.runtime.SuspendedAttachableDotNetRuntime
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfo

class AzureFunctionsDotNetCoreRuntime(val coreToolsInfo: FunctionsCoreToolsInfo)
    : DotNetRuntime(DotNetCoreRuntimeType), SuspendedAttachableDotNetRuntime {

    override fun createDebugState(dotNetExecutable: DotNetExecutable, executionEnvironment: ExecutionEnvironment) : IDotNetDebugProfileState {
        return AzureFunctionsDotNetCoreDebugProfile(dotNetExecutable, executionEnvironment, coreToolsInfo.coreToolsExecutable, coreToolsInfo.coreToolsPath)
    }

    override fun createRunState(dotNetExecutable: DotNetExecutable, executionEnvironment: ExecutionEnvironment): RunProfileState {
        return DotNetProcessRunProfileState(dotNetExecutable, this, executionEnvironment)
    }

    override fun patchRunCommandLine(commandLine: GeneralCommandLine, runtimeArguments: List<String>) {
        if (commandLine.exePath.endsWith(".dll", true)) {
            val exePath = commandLine.exePath
            commandLine.parametersList.addAt(0, exePath)
            for (arg in runtimeArguments.reversed()) {
                commandLine.parametersList.addAt(0, arg)
            }
            commandLine.withExePath(coreToolsInfo.coreToolsExecutable)
        }
    }

    override fun createDebugAttachSuspendedState(
            runningAssemblyInfo: RunningAssemblyInfo,
            executionEnvironment: ExecutionEnvironment,
            consoleInitializer: (ConsoleViewImpl) -> Unit
    ) = AzureFunctionsDotNetCoreAttachSuspendedProfileState(this, runningAssemblyInfo, executionEnvironment, consoleInitializer)
}