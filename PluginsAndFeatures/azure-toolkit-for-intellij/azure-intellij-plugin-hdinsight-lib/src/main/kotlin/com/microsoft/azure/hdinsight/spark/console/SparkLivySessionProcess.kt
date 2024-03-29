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

import com.google.common.net.HostAndPort
import com.intellij.remote.RemoteProcess
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session
import rx.Observable
import java.io.InputStream
import java.io.OutputStream

class SparkLivySessionProcess(
        private val rxSchedulers: IdeSchedulers,
        val session: Session
) : RemoteProcess(), ILogger {
    override fun isDisconnected(): Boolean = session.isStop

    override fun getLocalTunnel(remotePort: Int): HostAndPort? = null

    override fun killProcessTree(): Boolean = true

    private val stdOutStream: InputStream = SparkLivySessionStdOutStream(session)
    private val stdErrStream: InputStream = SparkLivySessionStdErrStream(session)
    private val stdInStream: OutputStream = SparkLivySessionOutputStream(session)

    override fun waitFor(): Int = 0

    override fun destroy() {
        session.close()
        outputStream.close()
        errorStream.close()
        inputStream.close()
    }

    override fun getOutputStream(): OutputStream = stdInStream

    override fun getErrorStream(): InputStream = stdErrStream

    override fun exitValue(): Int {
        // FIXME!!! return -1 for exceptions got
        return 0
    }

    override fun setWindowSize(columns: Int, rows: Int) {

    }

    override fun getInputStream(): InputStream = stdOutStream

    fun start(): Observable<Session> = session.deploy()
            .subscribeOn(rxSchedulers.processBarVisibleAsync(
                    "Deploy Livy interactive console artifacts dependencies..."))
            .observeOn(rxSchedulers.processBarVisibleAsync(
                    "Create Spark Livy interactive console session..."))
            .flatMap { it.create() }
            .flatMap { it.awaitReady(rxSchedulers.processBarVisibleAsync(
                    "The Spark Livy interactive console session is starting..." )) }
            .doOnError { destroy() }
}