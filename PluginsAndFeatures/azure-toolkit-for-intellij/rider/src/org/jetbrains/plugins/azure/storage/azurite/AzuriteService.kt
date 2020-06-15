/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.storage.azurite

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.services.ServiceEventListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File

class AzuriteService {

    private val logger = Logger.getInstance(AzuriteService::class.java)

    var processHandler: ColoredProcessHandler? = null
        get() = field
        private set(value) {
            field = value
        }

    var workspace: String? = null
        get() = field
        private set(value) {
            field = value
        }

    val isRunning: Boolean
        get() {
            val currentHandler = processHandler
            return currentHandler != null && !currentHandler.isProcessTerminated
        }

    fun start(commandLine: GeneralCommandLine, workspaceLocation: String) {
        if (processHandler != null) {
            logger.warn("start() was called while processHandler was not null. The caller should verify if an existing session is running, before calling start()")
            return
        }

        processHandler = ColoredProcessHandler(commandLine)
        workspace = workspaceLocation
        processHandler?.let {
            it.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(p0: ProcessEvent, p1: Key<*>) { }

                override fun processTerminated(e: ProcessEvent) {
                    processHandler = null
                }

                override fun startNotified(e: ProcessEvent) { }
            })
            it.startNotify()
            syncServices()
        }
    }

    fun clean(workspace: File) {
        if (isRunning) stop()

        try {
            workspace.deleteRecursively()
            workspace.mkdir()
        } catch (e: Exception) {
            logger.error("Error during clean", e)
        }
    }

    fun stop() {
        if (processHandler == null) return

        processHandler?.let {
            try {
                it.destroyProcess()
            } finally {
                if (!it.isProcessTerminating && !it.isProcessTerminated) {
                    it.killProcess()
                }
                syncServices()
            }
        }
    }

    private fun syncServices() =
            ApplicationManager.getApplication().messageBus.syncPublisher(ServiceEventListener.TOPIC)
                    .handle(ServiceEventListener.ServiceEvent.createResetEvent(AzuriteServiceViewContributor.AzuriteSession::class.java))
}