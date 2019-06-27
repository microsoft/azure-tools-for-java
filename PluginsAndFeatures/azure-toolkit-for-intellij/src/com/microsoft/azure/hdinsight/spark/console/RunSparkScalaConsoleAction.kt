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

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.action.RunConfigurationActionUtils
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfigurationType
import com.microsoft.azuretools.ijidea.utility.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation
import com.microsoft.intellij.telemetry.TelemetryKeys
import com.microsoft.intellij.util.runInReadAction
import org.jetbrains.plugins.scala.console.RunConsoleAction
import org.jetbrains.plugins.scala.console.ScalaConsoleRunConfigurationFactory
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import scala.Function1
import scala.runtime.BoxedUnit

abstract class RunSparkScalaConsoleAction
    : AzureAnAction(), RunConsoleAction.RunActionBase<LivySparkBatchJobRunConfigurationType>, ILogger {
    abstract val consoleRunConfigurationFactory: ScalaConsoleRunConfigurationFactory

    abstract val selectedMenuActionId: String

    override fun getServiceName(event: AnActionEvent?): String {
        val project = event?.project ?: return super.getServiceName(event)
        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration
        return (selectedConfigSettings?.configuration as LivySparkBatchJobRunConfiguration).sparkApplicationType.value
    }

    override fun getOperationName(event: AnActionEvent?): String {
        // FIXME: set operation name
        return super.getOperationName(event)
    }

    override fun onActionPerformed(event: AnActionEvent, operation: Operation?): Boolean {
        val dataContext = event.dataContext
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return true

        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration

        // Try current selected Configuration
        (selectedConfigSettings?.configuration as? LivySparkBatchJobRunConfiguration)?.run {
            runExisting(selectedConfigSettings, runManagerEx, operation)
            return false
        }

        val batchConfigurationType = SelectSparkApplicationTypeAction.getRunConfigurationType()
        if (batchConfigurationType == null) {
            val action = ActionManagerEx.getInstance().getAction(selectedMenuActionId)
            action?.actionPerformed(event)
            operation?.complete()
            return false
        }

        val batchConfigSettings = runManagerEx.getConfigurationSettingsList(batchConfigurationType)
        if (batchConfigSettings.isNotEmpty()) {
            // Find one from the same type list
            runExisting(batchConfigSettings[0], runManagerEx, operation)
        } else {
            // Create a new one to run
            createAndRun(batchConfigurationType, runManagerEx, project, newSettingName, runConfigurationHandler, operation)
        }
        return false
    }

    private fun createAndRun(
            configurationType: ConfigurationType,
            runManagerEx: RunManagerEx,
            project: Project,
            name: String,
            handler: Function1<RunConfiguration, BoxedUnit>,
            operation: Operation?) {
        runInReadAction {
            val factory = configurationType.configurationFactories[0]
            val setting = RunManager.getInstance(project).createConfiguration(name, factory)

            // Newly created config should let the user to edit
            setting.isEditBeforeRun = true
            handler.apply(setting.configuration)
            runFromSetting(setting, runManagerEx, operation)

            // Skip edit the next time
            setting.isEditBeforeRun = false
        }
    }

    private fun runExisting(setting: RunnerAndConfigurationSettings,
                            runManagerEx: RunManagerEx,
                            operation: Operation?) {
        runInReadAction {
            runFromSetting(setting, runManagerEx, operation)
        }
    }

    abstract val focusedTabIndex: Int

    abstract val isLocalRunConfigEnabled: Boolean

    private fun runFromSetting(setting: RunnerAndConfigurationSettings,
                               runManagerEx: RunManagerEx,
                               operation: Operation?) {
        val configuration = setting.configuration
        runManagerEx.setTemporaryConfiguration(setting)

        if (configuration is LivySparkBatchJobRunConfiguration) {
            configuration.model.focusedTabIndex = focusedTabIndex
            configuration.model.isLocalRunConfigEnabled = isLocalRunConfigEnabled
        }

        val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(runExecutor, setting)
                .runProfile(consoleRunConfigurationFactory.createConfiguration(configuration.name, configuration))
                .build()
        environment.putUserData(TelemetryKeys.OPERATION, operation)

        RunConfigurationActionUtils.runEnvironmentProfileWithCheckSettings(environment)

        if (configuration is LivySparkBatchJobRunConfiguration) {
            configuration.model.isLocalRunConfigEnabled = true
        }
    }

    override fun getMyConfigurationType(): LivySparkBatchJobRunConfigurationType? =
        LivySparkBatchJobRunConfigurationType.getInstance()

    override fun checkFile(psiFile: PsiFile): Boolean = psiFile is ScalaFile
}
