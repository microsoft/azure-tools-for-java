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

import com.intellij.execution.Executor
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.roots.TestSourcesFilter.isTestSources
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azure.hdinsight.spark.run.getNormalizedClassNameForSpark
import com.microsoft.azure.hdinsight.spark.run.getSparkConfigurationContext
import com.microsoft.azure.hdinsight.spark.run.getSparkMainClassWithElement
import com.microsoft.azure.hdinsight.spark.run.isSparkContext
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.ErrorType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.telemetry.TelemetryKeys
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
                    ProgramRunner.getRunner(runExecutor.id, setting.configuration)
                            ?.canRun(runExecutor.id, setting.configuration) == true

    override fun update(actionEvent: AnActionEvent) {
        val presentation = actionEvent.presentation.apply { isEnabledAndVisible = false }

        val project = actionEvent.project ?: return
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration

        val mainClass = actionEvent.dataContext.getSparkConfigurationContext()?.getSparkMainClassWithElement()
        if (mainClass?.containingFile?.let { isTestSources(it.virtualFile, project) } == true) {
            return
        }

        presentation.apply {
            when {

                actionEvent.isFromActionToolbar -> isEnabledAndVisible = canRun(selectedConfigSettings?: return)
                else -> {
                    // From context menu or Line marker action menu
                    isEnabledAndVisible = actionEvent.dataContext.isSparkContext()

                    if (!isEnabledAndVisible) {
                        return@apply
                    }

                    // In Spark Context
                    if (selectedConfigSettings?.let { canRun(it) } == true) {
                        text = "${runExecutor.id} ${selectedConfigSettings.name}"
                        description = "Submit a Spark job with the current configuration for this main class"
                    } else {
                        /**
                         * FIXME with [LivySparkBatchJobRunConfiguration.suggestedName]
                         * to create a new run configuration to submit a Spark job for this main class
                         */
//                        val className = actionEvent.dataContext.getSparkConfigurationContext()
//                                ?.getSparkMainClassWithElement()
//                                ?.getNormalizedClassNameForSpark()
//                                ?: ""
//
//                        text = "${runExecutor.id} [Spark Job] $className"
                        isEnabledAndVisible = false
                    }
                }
            }
        }
    }

    override fun onActionPerformed(actionEvent: AnActionEvent, operation: Operation?): Boolean {
        try {
            val project = actionEvent.project ?: return true
            val runManagerEx = RunManagerEx.getInstanceEx(project)
            val selectedConfigSettings = runManagerEx.selectedConfiguration

            when {
                actionEvent.isFromActionToolbar -> {
                    // Try current selected Configuration
                    selectedConfigSettings?.also {
                        if (canRun(it)) {
                            runExisting(it, operation)
                        } else {
                            EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.userError, RuntimeConfigurationError("Not a runnable configuration"), null, null)
                        }
                    }
                }
                else -> {
                    // From context menu or Line marker action menu
                    if (!actionEvent.dataContext.isSparkContext()) {
                        // No action for out of Spark Context
                        EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.userError, RuntimeConfigurationError("Not in Spark context"), null, null)
                        return false
                    }

                    // In Spark Context
                    val className = actionEvent.dataContext.getSparkConfigurationContext()
                            ?.getSparkMainClassWithElement()
                            ?.getNormalizedClassNameForSpark()
                            ?: ""

                    if (selectedConfigSettings?.let { canRun(it) } == true) {
                        val savedIsEditBeforeRun = selectedConfigSettings.isEditBeforeRun

                        selectedConfigSettings.isEditBeforeRun = true

                        // canRun() has checked configuration is LivySparkBatchJobRunConfiguration or not
                        (selectedConfigSettings.configuration as LivySparkBatchJobRunConfiguration).submitModel.mainClassName =
                                className

                        runExisting(selectedConfigSettings, operation)

                        selectedConfigSettings.isEditBeforeRun = savedIsEditBeforeRun
                    } else {
                        EventUtil.logErrorClassNameOnlyWithComplete(operation, ErrorType.userError, RuntimeConfigurationError("Not a runnable configuration"), null, null)

                        /**
                         * FIXME with [LivySparkBatchJobRunConfiguration.suggestedName]
                         * to create a new run configuration to submit a Spark job for this main class
                         */
                    }
                }
            }
            return false
        } catch (ignored: RuntimeException) {
        }

        return true
    }

    private fun runExisting(setting: RunnerAndConfigurationSettings, operation: Operation?) {
        runInReadAction {
            runFromSetting(setting, operation)
        }
    }

    private fun runFromSetting(setting: RunnerAndConfigurationSettings, operation: Operation?) {
        val environment = ExecutionEnvironmentBuilder.create(runExecutor, setting).build()
        environment.putUserData(TelemetryKeys.OPERATION, operation)

        RunConfigurationActionUtils.runEnvironmentProfileWithCheckSettings(environment)
    }
}