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
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RemoteState
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.common.SparkLocalRunConfigurableModel
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.hdinsight.messages.HDInsightBundle

class SparkBatchLocalDebugState(myProject: Project, model: SparkLocalRunConfigurableModel, operation: Operation?) :
    SparkBatchLocalRunState(
        myProject,
        model,
        operation,
        HDInsightBundle.message("SparkRunConfigLocalDebugButtonClick")!!
    ), RemoteState {
    private val remoteConnection = RemoteConnection(true, "127.0.0.1", "0", true)

    override fun getRemoteConnection(): RemoteConnection = remoteConnection

    override fun getCommandLineVmParameters(executor: Executor?, params: JavaParameters, moduleName: String): List<String> {
        // TODO: Add onthrow and onuncaught with Breakpoint UI settings later
        val debugConnection = "-agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:${remoteConnection.applicationAddress},suspend=y"

        return super.getCommandLineVmParameters(executor, params, moduleName).plus(debugConnection)
    }
}