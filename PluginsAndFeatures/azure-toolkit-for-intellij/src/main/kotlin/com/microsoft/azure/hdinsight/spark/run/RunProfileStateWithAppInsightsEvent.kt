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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.microsoft.azuretools.telemetry.AppInsightsClient
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation

abstract class RunProfileStateWithAppInsightsEvent(val uuid: String,
                                                   val appInsightsMessage: String,
                                                   val operation: Operation?) : RunProfileState {
    fun getPostEventProperties(executor: Executor, addedEventProps: Map<String, String>?): Map<String, String> {
        return mapOf(
            "Executor" to executor.id,
            "ActionUuid" to uuid).plus(addedEventProps ?: emptyMap())
    }

    // FIXME: This is legacy telemetry API, please use createErrorEventWithComplete or createInfoEventWithComplete instead
    fun createAppInsightEvent(executor: Executor, addedEventProps: Map<String, String>?): RunProfileState {
        val postEventProps = getPostEventProperties(executor, addedEventProps)

        AppInsightsClient.create(appInsightsMessage, null, postEventProps)

        return this
    }

    fun createErrorEventWithComplete(executor: Executor?, exp: Throwable, errorType: ErrorType, addedEventProps: Map<String, String>?) {
        val postEventProps = executor?.let { getPostEventProperties(it, addedEventProps) } ?: emptyMap()
        EventUtil.logErrorClassNameOnlyWithComplete(operation, errorType, exp, postEventProps, null)
    }

    fun createInfoEventWithComplete(executor: Executor?, addedEventProps: Map<String, String>?) {
        val postEventProps = executor?.let { getPostEventProperties(it, addedEventProps) } ?: emptyMap()
        EventUtil.logEventWithComplete(EventType.info, operation, postEventProps, null)
    }
}