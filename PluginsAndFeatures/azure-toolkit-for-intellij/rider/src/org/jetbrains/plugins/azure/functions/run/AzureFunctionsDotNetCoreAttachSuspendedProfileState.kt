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

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachSuspendedStartInfo
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.runtime.RunningAssemblyInfo
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class AzureFunctionsDotNetCoreAttachSuspendedProfileState(
        private val runtime: AzureFunctionsDotNetCoreRuntime,
        private val runningAssemblyInfo: RunningAssemblyInfo,
        environment: ExecutionEnvironment,
        private val consoleInitializer: (ConsoleViewImpl) -> Unit
) : DotNetCoreAttachProfileState(runningAssemblyInfo.processInfo, environment) {

    override fun initializeConsole(consoleView: ConsoleViewImpl) = consoleInitializer(consoleView)

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase =
        DotNetCoreAttachSuspendedStartInfo(
                dotNetCoreRuntimeExecutable = runtime.coreToolsInfo.coreToolsExecutable,
                processId = processInfo.pid,
                threadId = runningAssemblyInfo.threadId,
                assemblyPath = runningAssemblyInfo.executableAssemblyPath
        )
}