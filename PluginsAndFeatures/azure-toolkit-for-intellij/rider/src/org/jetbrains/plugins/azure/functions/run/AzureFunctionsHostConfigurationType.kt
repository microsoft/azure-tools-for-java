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

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.jetbrains.rider.model.*
import com.jetbrains.rider.run.configurations.*
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsConfiguration
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration

class AzureFunctionsHostConfigurationType: ConfigurationTypeBase("AzureFunctionsHost", "Azure Functions host",
        "Azure Functions host", IconLoader.getIcon("icons/FunctionAppRunConfiguration.svg")), IRunnableProjectConfigurationType, IRunConfigurationWithDefault {

    companion object {
        fun isTypeApplicable(kind: RunnableProjectKind) =
                kind == RunnableProjectKind.AzureFunctions
    }

    private val factory: AzureFunctionsHostConfigurationFactory = AzureFunctionsHostConfigurationFactory(this)

    init {
        addFactory(factory)
    }

    override val priority: IRunConfigurationWithDefault.Priority
        get() = IRunConfigurationWithDefault.Priority.Top

    override fun isApplicable(kind: RunnableProjectKind) = isTypeApplicable(kind)

    override fun tryCreateDefault(projects: List<RunnableProject>, project: Project, runManager: RunManager): RunnerAndConfigurationSettings? =
            if (runManager.allConfigurationsList.all { it !is DotNetProjectConfiguration && it !is AzureFunctionsHostConfiguration && it !is LaunchSettingsConfiguration }
                    && projects.any { isApplicable(it.kind) }
                    && !projects.any {
                        // Don't create "default" config if launchSettings.json is available for any project
                        it.kind == RunnableProjectKind.LaunchSettings && LaunchSettingsJsonService.getLaunchSettingsFileForProject(it)?.exists() == true
                    }) {
                val defaultSettings = runManager.createConfiguration("Default", factory)
                runManager.addConfiguration(defaultSettings, false)
                defaultSettings
            } else {
                null
            }
}