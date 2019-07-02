/*
 * Copyright (c) Microsoft Corporation
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
 */

package com.microsoft.azure.hdinsight.spark.run

import com.intellij.debugger.engine.RemoteDebugProcessHandler
import com.intellij.execution.KillableProcess
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes.STDERR
import com.intellij.execution.process.ProcessOutputTypes.STDOUT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.microsoft.azure.hdinsight.common.MessageInfoType
import rx.subjects.PublishSubject
import java.util.AbstractMap.SimpleImmutableEntry

class SparkBatchJobDebugProcessHandler(project: Project,
                                       val remoteDebugProcess: SparkBatchJobRemoteProcess,
                                       debugEventSubject: PublishSubject<SparkBatchJobSubmissionEvent>)
    : RemoteDebugProcessHandler(project), SparkBatchJobProcessCtrlLogOut, KillableProcess {
    private var isKilled: Boolean = false

    private var isJdbPortForwarded: Boolean = false

    init {
        this.remoteDebugProcess.eventSubject
                .subscribe {
                    if (it is SparkBatchDebugJobJdbPortForwardedEvent) {
                        isJdbPortForwarded = true

                        debugEventSubject.onNext(SparkBatchRemoteDebugHandlerReadyEvent(this, it))
                    } else {
                        debugEventSubject.onNext(it)
                    }
                }

        addProcessListener(object: ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                // JDB Debugger is stopped, tell the debug process
                (event.processHandler as SparkBatchJobDebugProcessHandler).remoteDebugProcess.disconnect()
            }
        })
    }

    override fun killProcess() {
        // Just do it
        remoteDebugProcess.destroy()

        isKilled = true
    }

    override fun canKillProcess(): Boolean = true

    // True for killProcess() will be called after the Stop button is pressed
    override fun isProcessTerminating(): Boolean = !isJdbPortForwarded && !isKilled || super.isProcessTerminating()

    // True for the Stop button will be disabled, and Remote Debug button will be enabled
    override fun isProcessTerminated(): Boolean = isKilled || remoteDebugProcess.isDestroyed || super.isProcessTerminated()

    override fun getCtrlSubject(): PublishSubject<SimpleImmutableEntry<MessageInfoType, String>> {
        return remoteDebugProcess.ctrlSubject
    }

    override fun startNotify() {
        addProcessListener(object : ProcessAdapter() {
            override fun startNotified(event: ProcessEvent) {
                val stdoutReader = SparkSimpleLogStreamReader(this@SparkBatchJobDebugProcessHandler, remoteDebugProcess.inputStream, STDOUT)
                val stderrReader = SparkSimpleLogStreamReader(this@SparkBatchJobDebugProcessHandler, remoteDebugProcess.errorStream, STDERR)

                remoteDebugProcess.ctrlSubject.subscribe(
                        { },
                        { },
                        {
                            // Stop readers when the process is finished
                            stderrReader.stop()
                            stdoutReader.stop()

                            try {
                                stderrReader.waitFor()
                                stdoutReader.waitFor()
                            } catch (ignore: InterruptedException) {
                            } finally {
                                removeProcessListener(this)
                            }
                        })
            }
        })

        super.startNotify()
    }

    override fun detachIsDefault(): Boolean = false

    override fun <T> getUserDataDelegate(key: Key<T>): T? {
        return this.getUserData(key)
    }
}
