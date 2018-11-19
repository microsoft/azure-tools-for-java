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

import com.intellij.execution.*
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfiguration
import com.microsoft.azure.hdinsight.spark.run.configuration.RemoteDebugRunConfigurationType
import com.microsoft.intellij.util.runInReadAction
import org.jetbrains.plugins.scala.console.RunConsoleAction
import org.jetbrains.plugins.scala.console.ScalaConsoleRunConfigurationFactory
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import scala.Function1
import scala.runtime.BoxedUnit

abstract class RunSparkScalaConsoleAction
    : AnAction(), RunConsoleAction.RunActionBase<RemoteDebugRunConfigurationType>, ILogger {
    abstract val consoleRunConfigurationFactory: ScalaConsoleRunConfigurationFactory

    override fun actionPerformed(event: AnActionEvent) {
        val dataContext = event.dataContext
        val file = CommonDataKeys.PSI_FILE.getData(dataContext)
        val project = CommonDataKeys.PROJECT.getData(dataContext)

        if (file == null || project == null || !checkFile(file)) {
            return
        }

        val runManagerEx = RunManagerEx.getInstanceEx(project)
        val selectedConfigSettings = runManagerEx.selectedConfiguration

        // Try current selected Configuration
        (selectedConfigSettings?.configuration as? RemoteDebugRunConfiguration)?.run {
            runExisting(selectedConfigSettings, runManagerEx, project)
            return
        }

        val batchConfigurationType = findConfigurationType(RemoteDebugRunConfigurationType::class.java)
        val batchConfigSettings = runManagerEx.getConfigurationSettingsList(batchConfigurationType)

        // Try to find one from the same type list
        batchConfigSettings.forEach {
            runExisting(it, runManagerEx, project)
            return
        }

        // Create a new one to run
        createAndRun(batchConfigurationType, runManagerEx, project, newSettingName, runConfigurationHandler)
    }

    private fun createAndRun(
            configurationType: ConfigurationType,
            runManagerEx: RunManagerEx,
            project: Project,
            name: String,
            handler: Function1<RunConfiguration, BoxedUnit>) {
        runInReadAction {
            val factory = configurationType.configurationFactories[0]
            val setting = RunManager.getInstance(project).createConfiguration(name, factory)
            handler.apply(setting.configuration)
            runFromSetting(project, setting, runManagerEx)
        }
    }

    private fun runExisting(setting: RunnerAndConfigurationSettings, runManagerEx: RunManagerEx, project: Project) {
        runInReadAction {
            runFromSetting(project, setting, runManagerEx)
        }
    }

    private fun runFromSetting(project: Project, setting: RunnerAndConfigurationSettings, runManagerEx: RunManagerEx) {
        val configuration = setting.configuration
        runManagerEx.setTemporaryConfiguration(setting)
        val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
        val runner = RunnerRegistry.getInstance().getRunner(runExecutor.id, configuration)
        if (runner != null) {
            try {
                val batchRunConfiguration = setting.configuration as? RemoteDebugRunConfiguration

                if (batchRunConfiguration == null) {
                    log().warn("Can't find Spark Run Configuration to start console")

                    return
                }

                val environment = ExecutionEnvironmentBuilder.create(
                        runExecutor,
                        consoleRunConfigurationFactory.createConfiguration(
                                batchRunConfiguration.name, batchRunConfiguration)).build()

                checkSettingsBeforeRun(environment.runProfile)

                runner.execute(environment)
            } catch (e: RuntimeConfigurationException) {
                Messages.showErrorDialog(project, "Can't start Spark Console since the Run Configuration file has errors: ${e.message}", ExecutionBundle.message("error.common.title"))
            } catch (e: ExecutionException) {
                Messages.showErrorDialog(project, e.message, ExecutionBundle.message("error.common.title"))
            }
        }
    }

    open fun checkSettingsBeforeRun(runProfile: RunProfile?) {
        (runProfile as? AbstractRunConfiguration)?.checkSettingsBeforeRun()
    }

    override fun getMyConfigurationType(): RemoteDebugRunConfigurationType? =
        findConfigurationType(RemoteDebugRunConfigurationType::class.java)

    override fun checkFile(psiFile: PsiFile): Boolean = psiFile is ScalaFile
}