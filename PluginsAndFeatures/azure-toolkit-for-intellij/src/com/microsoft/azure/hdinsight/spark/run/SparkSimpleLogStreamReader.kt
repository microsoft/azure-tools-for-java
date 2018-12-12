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

import com.intellij.execution.process.ProcessHandler
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.BaseOutputReader
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.Future

// A simple log reader to connect the input stream and process handler
class SparkSimpleLogStreamReader(val processHandler: ProcessHandler, inputStream: InputStream, private val logType: Key<*>)
    : BaseOutputReader(inputStream, Charset.forName("UTF-8")) {

    init {
        start("Reading Spark log " + logType.toString())
    }

    override fun onTextAvailable(s: String) {
        // Call process handler's text notify
        processHandler.notifyTextAvailable(s, logType)
    }

    override fun executeOnPooledThread(runnable: Runnable): Future<*> {
        return AppExecutorUtil.getAppExecutorService().submit(runnable)
    }
}