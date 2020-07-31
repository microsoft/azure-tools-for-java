/*
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base

import java.util.concurrent.ConcurrentHashMap

/**
 * Map is used to store state for a particular WebAppBase node when starting/stopping log streaming
 * An action is executed on an IDE side and all information are available on IDE side only.
 * We use this map to filter available Start/Stop Streaming Logs actions for a node based on state
 * (when log streaming is started - show Stop Logs Streaming, when log streaming is not started - Start Logs Streaming).
 */
object WebAppBaseStreamingLogs {

    private val streamingLogsMap = ConcurrentHashMap<String, Boolean>()

    fun isStreamingLogsStarted(webAppId: String): Boolean =
            streamingLogsMap[webAppId] ?: false

    fun setStreamingLogsStarted(webAppId: String, value: Boolean) {
        streamingLogsMap[webAppId] = value
    }

    fun clearStartedStreamingLogMap() {
        streamingLogsMap.clear()
    }
}
