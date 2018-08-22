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

import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.project.Project
import com.intellij.task.*
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration

class SparkScalaLivyConsoleProjectTaskRunner : ProjectTaskRunner() {
    override fun run(
            project: Project,
            context: ProjectTaskContext,
            callback: ProjectTaskNotification?,
            tasks: MutableCollection<out ProjectTask>
    ) {
        // TODO: Not called, why?

        callback?.finished(ProjectTaskResult(false, 0, 0))
    }

    override fun canRun(projectTask: ProjectTask): Boolean {
        val runConfiguration = (projectTask as? ExecuteRunConfigurationTask)?.runProfile

        return runConfiguration is RemoteDebugRunConfiguration
    }

    override fun createExecutionEnvironment(project: Project, projectTask: ExecuteRunConfigurationTask, executor: Executor?)
            : ExecutionEnvironment? {
        val batchRunConfiguration = (projectTask as? ExecuteRunConfigurationTask)?.runProfile as? RemoteDebugRunConfiguration
                ?: return null

        val environmentBuilder = ExecutionEnvironmentBuilder.create(
                executor ?: return null,
                SparkScalaLivyConsoleConfigurationType().confFactory().createConfiguration(
                        "${batchRunConfiguration.name} Spark livy console(Scala)", batchRunConfiguration))

        return environmentBuilder.build()
    }
}