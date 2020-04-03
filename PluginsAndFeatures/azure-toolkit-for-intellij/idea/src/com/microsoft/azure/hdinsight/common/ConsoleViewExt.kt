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
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.BrowserUtil
import com.microsoft.azure.hdinsight.common.classifiedexception.ClassifiedExceptionFactory
import com.microsoft.azure.hdinsight.spark.common.YarnDiagnosticsException
import com.microsoft.azure.hdinsight.spark.ui.ConsoleViewWithMessageBars
import java.net.URI
import java.util.AbstractMap.SimpleImmutableEntry

fun ConsoleView.print(text: String, messageInfoType: MessageInfoType) {
    // Redirect the remote process control message to console view
    when (messageInfoType) {
        MessageInfoType.Debug ->
            this.print("DEBUG: $text\n", ConsoleViewContentType.LOG_DEBUG_OUTPUT)
        MessageInfoType.Info ->
            this.print("INFO: $text\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        MessageInfoType.Warning ->
            this.print("WARN: $text\n", ConsoleViewContentType.LOG_WARNING_OUTPUT)
        MessageInfoType.Log ->
            this.print("LOG: $text\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        MessageInfoType.Hyperlink ->
            BrowserUtil.browse(URI.create(text))
        MessageInfoType.HtmlPersistentMessage ->
            this.print(text, ConsoleViewWithMessageBars.CONSOLE_VIEW_HTML_PERSISTENT_MESSAGE_TYPE)
        else -> {
            this.print("ERROR: $text\n", ConsoleViewContentType.ERROR_OUTPUT)

            val classifiedEx = ClassifiedExceptionFactory
                    .createClassifiedException(YarnDiagnosticsException(text))
            classifiedEx.logStackTrace()
            classifiedEx.handleByUser()
        }
    }
}

fun ConsoleView.print(typedMessage: SimpleImmutableEntry<MessageInfoType, String>) =
        this.print(typedMessage.value, typedMessage.key)