/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.rider.build.tasks.BuildProjectBeforeRunTask
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.run.configurations.DotNetConfigurationFactoryBase
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import org.jetbrains.plugins.azure.functions.buildTasks.BuildFunctionsProjectBeforeRunTaskProvider

class AzureFunctionsHostConfigurationFactory(type: ConfigurationType)
    : DotNetConfigurationFactoryBase<AzureFunctionsHostConfiguration>(type) {

    companion object {
        private const val FACTORY_ID = "AzureFunctionsHostFactory"
        private const val FACTORY_NAME = "Azure functions host factory"
    }

    override fun getId(): String = FACTORY_ID

    override fun getName(): String = FACTORY_NAME

    override fun configureBeforeRunTaskDefaults(providerID: Key<out BeforeRunTask<BeforeRunTask<*>>>?,
                                                task: BeforeRunTask<out BeforeRunTask<*>>?) {
        if (providerID == BuildFunctionsProjectBeforeRunTaskProvider.providerId && task is BuildProjectBeforeRunTask) {
            task.isEnabled = true
        }
    }

    override fun getSingletonPolicy(): RunConfigurationSingletonPolicy {
        return RunConfigurationSingletonPolicy.SINGLE_INSTANCE
    }

    private fun createParameters(project: Project) =
            AzureFunctionsHostConfigurationParameters(
                    project,
                    exePath = "",
                    programParameters = "",
                    workingDirectory = "",
                    envs = hashMapOf(),
                    isPassParentEnvs = true,
                    useExternalConsole = false,
                    projectFilePath = "",
                    trackProjectExePath = true,
                    trackProjectArguments = true,
                    trackProjectWorkingDirectory = true,
                    projectKind = RunnableProjectKind.None,
                    projectTfm = "",
                    functionNames = "",
                    startBrowserParameters = DotNetStartBrowserParameters()
            )

    override fun createConfiguration(name: String?, template: RunConfiguration): RunConfiguration =
            AzureFunctionsHostConfiguration(name
                    ?: "Azure Functions", template.project, this, createParameters(template.project))

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
            AzureFunctionsHostConfiguration("Azure Functions", project, this, createParameters(project))
}