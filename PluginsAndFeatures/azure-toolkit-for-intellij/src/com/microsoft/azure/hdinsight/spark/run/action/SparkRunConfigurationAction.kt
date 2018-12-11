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

package com.microsoft.azure.hdinsight.spark.run.action

import com.intellij.execution.*
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.intellij.util.runInReadAction
import javax.swing.Icon

abstract class SparkRunConfigurationAction : AzureAnAction, ILogger {
    constructor(icon: Icon?) : super(icon)
    constructor(text: String?) : super(text)
    constructor(text: String?, description: String?, icon: Icon?) : super(text, description, icon)
    constructor() : super()

    abstract val runExecutor: Executor

    open fun canRun(setting: RunnerAndConfigurationSettings): Boolean =
            setting.configuration is LivySparkBatchJobRunConfiguration &&
                    RunnerRegistry.getInstance().getRunner(runExecutor.id, setting.configuration) != null

    override fun update(actionEvent: AnActionEvent) {
        val presentation = actionEvent.presentation.apply { isEnabled = false }

        val project = actionEvent.project ?: return
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration ?: return

        presentation.isEnabled = canRun(selectedConfigSettings)
    }

    override fun onActionPerformed(actionEvent: AnActionEvent?) {
        val project = actionEvent?.project ?: return
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration ?: return

        // Try current selected Configuration
        if (!canRun(selectedConfigSettings)) {
            return
        }

        runExisting(selectedConfigSettings)
    }

    private fun runExisting(setting: RunnerAndConfigurationSettings) {
        runInReadAction {
            runFromSetting(setting)
        }
    }

    private fun runFromSetting(setting: RunnerAndConfigurationSettings) {
        val environment = ExecutionEnvironmentBuilder.create(runExecutor, setting).build()

        RunConfigurationActionUtils.runEnvironmentProfileWithCheckSettings(environment)
    }
}