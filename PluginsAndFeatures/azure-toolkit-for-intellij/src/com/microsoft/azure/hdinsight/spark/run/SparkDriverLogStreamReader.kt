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
import com.intellij.execution.process.ProcessOutputTypes.SYSTEM
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.io.BaseOutputReader
import com.microsoft.azure.hdinsight.common.ConsoleViewLogLine
import com.microsoft.azure.hdinsight.common.ConsoleViewTypeRegistration.Companion.contentTypeKeyMap
import com.microsoft.azure.hdinsight.common.MessageInfoType
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogLine
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogSource
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogUtils
import java.io.InputStream
import java.nio.charset.Charset
import java.util.concurrent.Future

class SparkDriverLogStreamReader(val processHandler: ProcessHandler,
                                 inputStream: InputStream,
                                 private val logSource: SparkBatchJobLogSource)
    : BaseOutputReader(inputStream, Charset.forName("UTF-8")) {
    private val defaultMessageInfoType = MessageInfoType.Log
    private var previousLogLine = SparkBatchJobLogLine(logSource, defaultMessageInfoType, "")

    init {
        start("Reading Spark Driver log $logSource")
    }

    override fun onTextAvailable(s: String) {
        val currentLogLine = SparkBatchJobLogLine(logSource, defaultMessageInfoType, s)
        val typedLogLine = SparkBatchJobLogUtils.mapTypedMessageByLog4jLevels(previousLogLine, currentLogLine)
        val consoleViewLogLine = ConsoleViewLogLine(typedLogLine)

        // The second parameter is of Key<Any> type and there are only 3 registered Keys: SYSTEM, STDOUT and STDERR.
        // To support more log style, we registered more Keys in class ConsoleViewTypeRegistration at application
        // startup
        processHandler.notifyTextAvailable(
                consoleViewLogLine.formatText, contentTypeKeyMap[consoleViewLogLine.contentType] ?: SYSTEM)

        // Update previous log line
        previousLogLine = typedLogLine
    }

    override fun executeOnPooledThread(runnable: Runnable): Future<*> {
        return AppExecutorUtil.getAppExecutorService().submit(runnable)
    }
}