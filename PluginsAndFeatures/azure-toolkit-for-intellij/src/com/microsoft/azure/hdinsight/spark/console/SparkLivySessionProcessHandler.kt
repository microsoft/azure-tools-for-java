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

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.process.*
import com.intellij.execution.process.ProcessOutputTypes.*
import com.intellij.openapi.util.Key
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.SparkSimpleLogStreamReader
import rx.subjects.PublishSubject
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8

class SparkLivySessionProcessHandler(val process: SparkLivySessionProcess)
        : ProcessHandler(), AnsiEscapeDecoder.ColoredTextAcceptor, ILogger {

    private val myAnsiEscapeDecoder = AnsiEscapeDecoder()
    private val sessionEventsSubject = PublishSubject.create<String>()

    override fun notifyTextAvailable(text: String, outputType: Key<*>) {
        myAnsiEscapeDecoder.escapeText(text, outputType, this)
    }

    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
        super.notifyTextAvailable(text, attributes)
    }

    override fun getProcessInput(): OutputStream? = process.outputStream

    override fun detachIsDefault(): Boolean = false

    override fun detachProcessImpl() {
        destroyProcessImpl()
    }

    override fun destroyProcessImpl() {
        process.destroy()
        notifyProcessTerminated(0)
        sessionEventsSubject.onCompleted()
    }

    fun execute(codes: String) {
        process.outputStream.write("$codes\n".toByteArray(UTF_8))
        process.outputStream.flush()
    }

    override fun startNotify() {
        notifyTextAvailable("Start Spark Livy Interactive Session Console in cluster ${process.session.baseUrl.host}...\n", SYSTEM)
        addProcessListener(object : ProcessAdapter() {
            override fun startNotified(event: ProcessEvent) {
                val stdoutReader = SparkSimpleLogStreamReader(this@SparkLivySessionProcessHandler, process.inputStream, STDOUT)
                val stderrReader = SparkSimpleLogStreamReader(this@SparkLivySessionProcessHandler, process.errorStream, STDERR)

                sessionEventsSubject.subscribe(
                        {},
                        { err -> log().warn("Spark Livy Session event error", err) },
                        {
                            try {
                                // Stop readers when the process is finished
                                stderrReader.stop()
                                stdoutReader.stop()

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
}