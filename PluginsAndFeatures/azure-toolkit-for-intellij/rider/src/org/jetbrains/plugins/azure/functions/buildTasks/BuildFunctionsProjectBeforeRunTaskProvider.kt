/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.buildTasks

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.Semaphore
import com.jetbrains.rd.platform.util.application
import com.jetbrains.rider.build.BuildHost
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.model.BuildResultKind
import com.jetbrains.rider.model.BuildTarget
import com.jetbrains.rider.util.idea.getComponent
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.run.AzureFunctionsHostConfiguration
import javax.swing.Icon

class BuildFunctionsProjectBeforeRunTask : BeforeRunTask<BuildFunctionsProjectBeforeRunTask>(BuildFunctionsProjectBeforeRunTaskProvider.providerId)

class BuildFunctionsProjectBeforeRunTaskProvider : BeforeRunTaskProvider<BuildFunctionsProjectBeforeRunTask>(){
    companion object {
        val providerId = Key.create<BuildFunctionsProjectBeforeRunTask>("BuildFunctionsProject")
    }

    override fun getId(): Key<BuildFunctionsProjectBeforeRunTask>? = providerId

    override fun getName(): String? = message("run_config.run.form.function_app.before_run_tasks.build_function_project_name")

    override fun getDescription(task: BuildFunctionsProjectBeforeRunTask?): String? = message("run_config.run.form.function_app.before_run_tasks.build_function_project_description")

    override fun isConfigurable(): Boolean {
        return false
    }

    override fun getIcon(): Icon = AllIcons.Actions.Compile

    private fun shouldCreateBuildBeforeRunTaskByDefault(runConfiguration: RunConfiguration): Boolean {
        return runConfiguration is AzureFunctionsHostConfiguration
    }

    override fun createTask(runConfiguration: RunConfiguration): BuildFunctionsProjectBeforeRunTask? {
        if (!shouldCreateBuildBeforeRunTaskByDefault(runConfiguration)) return null
        val task = BuildFunctionsProjectBeforeRunTask()
        task.isEnabled = true
        return task
    }

    override fun canExecuteTask(configuration: RunConfiguration, task: BuildFunctionsProjectBeforeRunTask): Boolean {
        return configuration.project.getComponent<BuildHost>().ready.value
    }

    override fun executeTask(context: DataContext, configuration: RunConfiguration, env: ExecutionEnvironment,
                             task: BuildFunctionsProjectBeforeRunTask): Boolean {
        val project = configuration.project
        val buildHost = project.getComponent<BuildHost>()
        val selectedProjectsForBuild = when (configuration) {
            is AzureFunctionsHostConfiguration -> listOf(configuration.parameters.projectFilePath)
            else -> emptyList()
        }
        if (!buildHost.ready.value)
            return false
        val finished = Semaphore()
        finished.down()
        var result = false
        // when false returned build was not started because another is in progress, we should not run task
        application.invokeLater {
            result = buildHost.requestBuild(BuildParameters(BuildTarget(), selectedProjectsForBuild)) {
                result = it == BuildResultKind.Successful || it == BuildResultKind.HasWarnings
                finished.up()
            }
        }
        finished.waitFor()
        return result
    }
}