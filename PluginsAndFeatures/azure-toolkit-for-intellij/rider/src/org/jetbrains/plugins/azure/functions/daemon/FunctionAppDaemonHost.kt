/**
 * Copyright (c) 2020 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.jetbrains.plugins.azure.functions.daemon

import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.platform.ui.bedsl.extensions.valueOrEmpty
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent
import com.jetbrains.rider.azure.model.functionAppDaemonModel
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import org.jetbrains.plugins.azure.functions.actions.TriggerAzureFunctionAction
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfiguration
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationFactory
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfigurationType
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsRunnableProjectUtil

@Suppress("ComponentNotRegistered")
class FunctionAppDaemonHost(project: Project) : LifetimedProjectComponent(project) {

    val model = project.solution.functionAppDaemonModel

    init {
        initRunFunctionAppHandler()
        initDebugFunctionAppHandler()
        initTriggerFunctionApp()
    }

    private fun initRunFunctionAppHandler() {
        model.runFunctionApp.advise(componentLifetime) { functionAppRequest ->
            runConfiguration(
                    functionName = functionAppRequest.functionName,
                    runnableProject = getRunnableProject(project, functionAppRequest.projectFilePath),
                    executor = DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    private fun initDebugFunctionAppHandler() {
        model.debugFunctionApp.advise(componentLifetime) { functionAppRequest ->
            runConfiguration(
                    functionName = functionAppRequest.functionName,
                    runnableProject = getRunnableProject(project, functionAppRequest.projectFilePath),
                    executor = DefaultDebugExecutor.getDebugExecutorInstance())
        }
    }

    private fun initTriggerFunctionApp() {
        model.triggerFunctionApp.advise(componentLifetime) { triggerFunctionRequest ->
            val triggerAction = TriggerAzureFunctionAction(functionName = triggerFunctionRequest.functionName)

            ActionUtil.invokeAction(
                    triggerAction,
                    SimpleDataContext.getProjectContext(project),
                    ActionPlaces.EDITOR_GUTTER_POPUP,
                    null,
                    null)
        }
    }

    private fun runConfiguration(
            functionName: String,
            runnableProject: RunnableProject,
            executor: Executor
    ) {
        val runManager = RunManager.getInstance(project)

        val existingSettings = findExistingConfigurationSettings(functionName, runnableProject.projectFilePath)

        // Take existing configuration or generate a new one if configuration is missing.
        val settings = existingSettings ?: let {
            val configuration = createFunctionAppRunConfiguration(project, functionName, runnableProject)
            val newSettings = runManager.createConfiguration(configuration, configuration.factory as AzureFunctionsHostConfigurationFactory)

            runManager.setTemporaryConfiguration(newSettings)
            runManager.addConfiguration(newSettings)

            newSettings
        }

        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, executor)
    }

    private fun findExistingConfigurationSettings(
            functionName: String,
            projectFilePath: String
    ): RunnerAndConfigurationSettings? {
        val runManager = RunManager.getInstance(project)

        val configurationType = ConfigurationTypeUtil.findConfigurationType(AzureFunctionsHostConfigurationType::class.java)
        val runConfigurations = runManager.getConfigurationsList(configurationType)

        return runConfigurations.filterIsInstance<AzureFunctionsHostConfiguration>().firstOrNull { configuration ->
            configuration.parameters.functionNames == functionName &&
                    configuration.parameters.projectFilePath == projectFilePath
        }?.let { configuration ->
            runManager.findSettings(configuration)
        }
    }

    private fun createFunctionAppRunConfiguration(
            project: Project,
            functionName: String,
            runnableProject: RunnableProject
    ): AzureFunctionsHostConfiguration {

        val runManager = RunManager.getInstance(project)
        val configurationType = ConfigurationTypeUtil.findConfigurationType(AzureFunctionsHostConfigurationType::class.java)

        val factory = configurationType.factory
        val template = runManager.getConfigurationTemplate(factory)

        val configuration = template.configuration as AzureFunctionsHostConfiguration
        patchConfigurationParameters(configuration, runnableProject, functionName)

        return configuration
    }

    private fun patchConfigurationParameters(
            configuration: AzureFunctionsHostConfiguration,
            runnableProject: RunnableProject,
            functionName: String
    ) {
        val prjToConfigure = AzureFunctionsRunnableProjectUtil.patchRunnableProjectOutputs(runnableProject)
        val projectOutput = prjToConfigure.projectOutputs.singleOrNull()

        configuration.name = "$functionName (${prjToConfigure.fullName})"
        configuration.parameters.functionNames = functionName

        configuration.parameters.projectFilePath = prjToConfigure.projectFilePath
        configuration.parameters.projectKind = prjToConfigure.kind
        configuration.parameters.projectTfm = projectOutput?.tfm ?: ""
        configuration.parameters.exePath = projectOutput?.exePath ?: ""
        configuration.parameters.programParameters = ParametersListUtil.join(projectOutput?.defaultArguments
                ?: listOf())
        configuration.parameters.workingDirectory = projectOutput?.workingDirectory ?: ""
    }

    private fun getRunnableProject(
            project: Project,
            expectedProjectPath: String
    ): RunnableProject {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrEmpty()
        return runnableProjects.find { runnableProject -> runnableProject.projectFilePath == expectedProjectPath }
                ?: throw IllegalStateException(
                        "Unable to find a project to run with path: '$expectedProjectPath', available project paths: " +
                                runnableProjects.joinToString(", ", "'", "'") { it.projectFilePath})
    }
}
