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

import com.intellij.execution.ui.ConsoleView
import com.intellij.ide.BrowserUtil
import com.microsoft.azure.hdinsight.common.classifiedexception.ClassifiedExceptionFactory
import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException
import com.microsoft.azure.hdinsight.spark.common.log.SparkLogLine
import java.net.URI

fun ConsoleView.print(typedMessage: SparkLogLine) {
    // Redirect the remote process control message to console view
    val consoleViewLogLine = ConsoleViewLogLine(typedMessage)
    when (typedMessage.messageInfoType) {
        MessageInfoType.Debug,
        MessageInfoType.Info,
        MessageInfoType.Warning,
        MessageInfoType.HtmlPersistentMessage,
        MessageInfoType.Log ->
            this.print(consoleViewLogLine.formatText, consoleViewLogLine.contentType)
        MessageInfoType.Hyperlink ->
            BrowserUtil.browse(URI.create(typedMessage.rawLog))
        else -> {
            this.print(consoleViewLogLine.formatText, consoleViewLogLine.contentType)

            val classifiedEx = ClassifiedExceptionFactory
                    .createClassifiedException(YarnDiagnosticsException(typedMessage.rawLog))
            classifiedEx.logStackTrace()
            classifiedEx.handleByUser()
        }
    }
}
