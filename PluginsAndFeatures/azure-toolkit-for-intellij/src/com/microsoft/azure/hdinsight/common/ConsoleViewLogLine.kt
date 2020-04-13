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

package com.microsoft.azure.hdinsight.common

import com.intellij.execution.ui.ConsoleViewContentType
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogLine
import com.microsoft.azure.hdinsight.spark.ui.ConsoleViewWithMessageBars

class ConsoleViewLogLine {
    val formatText: String
    val contentType: ConsoleViewContentType
    companion object {
        @JvmStatic val messageInfoTypeToConsoleViewContentType = mapOf(
                MessageInfoType.Debug to ConsoleViewContentType.LOG_DEBUG_OUTPUT,
                MessageInfoType.Info to ConsoleViewContentType.LOG_INFO_OUTPUT,
                MessageInfoType.Warning to ConsoleViewContentType.LOG_WARNING_OUTPUT,
                MessageInfoType.Log to ConsoleViewContentType.SYSTEM_OUTPUT,
                MessageInfoType.HtmlPersistentMessage to
                        ConsoleViewWithMessageBars.CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_TYPE
        )
    }

    constructor(sparkBatchJobLogLine: SparkBatchJobLogLine) {
        this.formatText =
                if (sparkBatchJobLogLine.messageInfoType == MessageInfoType.HtmlPersistentMessage) {
                    sparkBatchJobLogLine.rawLog
                } else {
                    "${sparkBatchJobLogLine.logSource}: ${sparkBatchJobLogLine.rawLog}".trimEnd('\n') + "\n"
                }
        this.contentType = messageInfoTypeToConsoleViewContentType.getOrDefault(
                sparkBatchJobLogLine.messageInfoType, ConsoleViewContentType.ERROR_OUTPUT)
    }
}