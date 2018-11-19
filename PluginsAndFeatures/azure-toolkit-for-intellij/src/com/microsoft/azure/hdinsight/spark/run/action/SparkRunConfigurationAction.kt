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
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration
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
            setting.configuration is RemoteDebugRunConfiguration &&
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

        runExisting(selectedConfigSettings, project)
    }

    private fun runExisting(setting: RunnerAndConfigurationSettings, project: Project) {
        runInReadAction {
            runFromSetting(project, setting)
        }
    }

    private fun runFromSetting(project: Project, setting: RunnerAndConfigurationSettings) {
        val configuration = setting.configuration
        val runner = RunnerRegistry.getInstance().getRunner(runExecutor.id, configuration) ?: return

        try {
            val environment = ExecutionEnvironmentBuilder.create(runExecutor, configuration).build()

            try {
                checkRunnerSettings(environment.runProfile, runner)
                setting.isEditBeforeRun = false
            } catch (configError: RuntimeConfigurationException) {
                log().warn("Found configuration error $configError, pop up run configuration dialog")
                setting.isEditBeforeRun = true
            }

            ProgramRunnerUtil.executeConfiguration(setting, runExecutor)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, e.message, ExecutionBundle.message("error.common.title"))
        }
    }

    open fun checkRunnerSettings(runProfile: RunProfile?, runner: ProgramRunner<RunnerSettings>) {
        (runProfile as? RemoteDebugRunConfiguration)?.checkRunnerSettings(runner, null, null)
    }

}