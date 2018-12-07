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

import com.intellij.remote.ColoredRemoteProcessHandler
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets.UTF_8

class SparkLivySessionProcessHandler(val process: SparkLivySessionProcess) :
        ColoredRemoteProcessHandler<SparkLivySessionProcess>(
                process, "Start Spark Livy Interactive Session Console in cluster ${process.session.baseUrl.host}...", UTF_8) {
    override fun getProcessInput(): OutputStream? = process.outputStream

    override fun detachIsDefault(): Boolean = false

    override fun detachProcessImpl() {
        destroyProcessImpl()
    }

    override fun destroyProcessImpl() {
        process.destroy()
    }

    fun execute(codes: String) {
        process.outputStream.write("$codes\n".toByteArray(UTF_8))
        process.outputStream.flush()
    }
}