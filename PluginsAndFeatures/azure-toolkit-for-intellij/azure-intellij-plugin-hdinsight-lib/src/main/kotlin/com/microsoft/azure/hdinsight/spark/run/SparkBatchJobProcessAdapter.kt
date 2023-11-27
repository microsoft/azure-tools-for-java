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

import com.google.common.net.HostAndPort
import com.intellij.remote.RemoteProcess
import java.io.InputStream
import java.io.OutputStream

class SparkBatchJobProcessAdapter(val sparkJobProcess: SparkBatchJobRemoteProcess)
    : RemoteProcess() {
    override fun destroy() {
        sparkJobProcess.destroy()
    }

    override fun exitValue(): Int {
        return sparkJobProcess.exitValue()
    }

    override fun isDisconnected(): Boolean {
        return sparkJobProcess.isDisconnected
    }

    override fun waitFor(): Int {
        return sparkJobProcess.waitFor()
    }

    override fun getLocalTunnel(i: Int): HostAndPort? {
        return sparkJobProcess.getLocalTunnel(i)
    }

    override fun getOutputStream(): OutputStream {
        return sparkJobProcess.outputStream
    }

    override fun getErrorStream(): InputStream {
        return sparkJobProcess.errorStream
    }

    override fun getInputStream(): InputStream {
        return sparkJobProcess.inputStream
    }

    override fun killProcessTree(): Boolean {
        return sparkJobProcess.killProcessTree()
    }

    override fun setWindowSize(columns: Int, rows: Int) {

    }
}