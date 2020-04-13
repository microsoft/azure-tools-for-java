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
import com.intellij.execution.ui.ConsoleViewContentType.*
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.util.Key

/**
 * The default registered ConsoleViewContentType has 3 items: SYSTEM, STDOUT and STDERR. It restricts the log type when
 * we print log through the stream bound to console view. Here we register console view types at IntelliJ application
 * startup so that SparkDriverLogStreamReader can use these keys and ConsoleViewRunningState::print can recognize
 * these keys. Therefore, console view can print logs with more registered types.
 */
class ConsoleViewTypeRegistration: ApplicationComponent {
    companion object {
        private val LOG_WARNING_OUTPUT_KEY = Key<Any>(ConsoleViewContentType.LOG_WARNING_OUTPUT.toString())
        private val LOG_INFO_OUTPUT_KEY = Key<Any>(ConsoleViewContentType.LOG_INFO_OUTPUT.toString())
        private val LOG_ERROR_OUTPUT_KEY = Key<Any>(ConsoleViewContentType.LOG_ERROR_OUTPUT.toString())

        val contentTypeKeyMap = mapOf(
                LOG_ERROR_OUTPUT to LOG_ERROR_OUTPUT_KEY,
                LOG_WARNING_OUTPUT to LOG_WARNING_OUTPUT_KEY,
                LOG_INFO_OUTPUT to LOG_INFO_OUTPUT_KEY)
    }

    override fun initComponent() {
        registerNewConsoleViewType(LOG_ERROR_OUTPUT_KEY, LOG_ERROR_OUTPUT)
        registerNewConsoleViewType(LOG_WARNING_OUTPUT_KEY, LOG_WARNING_OUTPUT)
        registerNewConsoleViewType(LOG_INFO_OUTPUT_KEY, LOG_INFO_OUTPUT)
    }
}