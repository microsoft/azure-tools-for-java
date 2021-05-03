/**
 * Copyright (c) 2021 JetBrains s.r.o.
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

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rdclient.util.idea.pumpMessages
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.debugger.DebuggerWorkerPlatform
import com.jetbrains.rider.debugger.DebuggerWorkerProcessHandler
import com.jetbrains.rider.model.debuggerWorker.DebuggerStartInfoBase
import com.jetbrains.rider.model.debuggerWorker.DotNetCoreAttachStartInfo
import com.jetbrains.rider.run.*
import com.jetbrains.rider.runtime.DotNetExecutable
import com.microsoft.intellij.util.PluginUtil
import org.apache.commons.lang.StringUtils
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.time.Duration

class AzureFunctionsDotNetCoreIsolatedDebugProfile(
        private val dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment)
    : DebugProfileStateBase(executionEnvironment) {

    companion object {
        private val logger = Logger.getInstance(AzureFunctionsDotNetCoreIsolatedDebugProfile::class.java)
        private const val DOTNET_ISOLATED_DEBUG_ARGUMENT = "--dotnet-isolated-debug"
    }

    private var processId = 0
    private lateinit var targetProcessHandler: ProcessHandler
    private lateinit var console: ConsoleView

    override suspend fun createWorkerRunInfo(lifetime: Lifetime, helper: DebuggerHelperHost, port: Int): WorkerRunInfo {
        // Launch Azure Functions host process
        launchAzureFunctionsHost()

        // Wait until we get a process ID (or the process terminates)
        pumpMessages(Duration.ofMinutes(2)) {
            processId != 0 || targetProcessHandler.isProcessTerminated
        }
        if (processId == 0) {
            logger.error("Azure Functions host did not return isolated worker process id.")

            // If we do not get pid from the isolated worker process, destroy the process here
            if (!targetProcessHandler.isProcessTerminating && !targetProcessHandler.isProcessTerminated) {
                targetProcessHandler.destroyProcess()
            }

            // Notify user
            PluginUtil.showErrorNotificationProject(
                    executionEnvironment.project,
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.title"),
                    RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_pid_unspecified"))

            // Throw and bail out early
            throw RuntimeException(RiderAzureBundle.message("run_config.run_function_app.debug.notification.isolated_worker_pid_unspecified"))
        }

        // Create debugger worker info
        return createWorkerRunInfoFor(port, DebuggerWorkerPlatform.AnyCpu)
    }

    private fun launchAzureFunctionsHost() {
        val useExternalConsole = dotNetExecutable.useExternalConsole

        val programParameters = ParametersListUtil.parse(dotNetExecutable.programParameterString)
        if (!programParameters.contains(DOTNET_ISOLATED_DEBUG_ARGUMENT)) {
            programParameters.add(DOTNET_ISOLATED_DEBUG_ARGUMENT)
        }

        val commandLine = createEmptyConsoleCommandLine(useExternalConsole)
                .withExePath(dotNetExecutable.exePath)
                .withWorkDirectory(dotNetExecutable.workingDirectory)
                .withEnvironment(dotNetExecutable.environmentVariables)
                .withRawParameters(ParametersListUtil.join(programParameters))

        targetProcessHandler = if (useExternalConsole) {
            ExternalConsoleMediator.createProcessHandler(commandLine)
        } else {
            TerminalProcessHandler(commandLine)
        }

        val commandLineString = commandLine.commandLineString

        logger.info("Starting functions host process with command line: $commandLineString")
        targetProcessHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) = logger.info("Process terminated: $commandLineString")

            override fun startNotified(event: ProcessEvent) {
                logger.info("Started functions host process")
                super.startNotified(event)
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val line = event.text

                if (processId == 0 &&
                        StringUtils.containsIgnoreCase(line, "Azure Functions .NET Worker (PID: ") &&
                        StringUtils.containsIgnoreCase(line, ") initialized")) {

                    val pidFromLog = line.substringAfter("PID: ")
                            .dropWhile { !it.isDigit() }
                            .takeWhile { it.isDigit() }
                            .toInt()

                    logger.info("Functions isolated worker process id: $pidFromLog")
                    processId = pidFromLog
                }

                super.onTextAvailable(event, outputType)
            }
        })

        console = createConsole(
                external = useExternalConsole,
                processHandler = targetProcessHandler,
                commandLineString = commandLine.commandLineString,
                project = executionEnvironment.project
        )

        console.attachToProcess(targetProcessHandler)

        targetProcessHandler.startNotify()
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler): ExecutionResult {
        throw UnsupportedOperationException("Use overload with lifetime")
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>, workerProcessHandler: DebuggerWorkerProcessHandler, lifetime: Lifetime): ExecutionResult {
        workerProcessHandler.attachTargetProcess(targetProcessHandler)
        return DefaultExecutionResult(console, workerProcessHandler)
    }

    override val consoleKind: ConsoleKind = if (dotNetExecutable.useExternalConsole)
        ConsoleKind.ExternalConsole else ConsoleKind.Normal

    override val attached: Boolean = false

    override fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun createModelStartInfo(lifetime: Lifetime): DebuggerStartInfoBase
        = DotNetCoreAttachStartInfo(processId, false)
}