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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.run.configurations.IRunConfigurationWithDefault
import com.jetbrains.rider.run.configurations.IRunnableProjectConfigurationType
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsConfiguration
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfiguration
import com.microsoft.icons.CommonIcons
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise
import org.jetbrains.plugins.azure.RiderAzureBundle.message

class AzureFunctionsHostConfigurationType : ConfigurationTypeBase(
        id = "AzureFunctionsHost",
        displayName = message("run_config.run_function_app.form.function_app.type_name"),
        description = message("run_config.run_function_app.form.function_app.type_description"),
        icon = CommonIcons.AzureFunctions.FunctionAppRunConfiguration
), IRunnableProjectConfigurationType, IRunConfigurationWithDefault {

    companion object {
        val instance get() = runConfigurationType<AzureFunctionsHostConfigurationType>()

        fun isTypeApplicable(kind: RunnableProjectKind): Boolean =
                kind == RunnableProjectKind.AzureFunctions
    }

    val factory: AzureFunctionsHostConfigurationFactory = AzureFunctionsHostConfigurationFactory(this)

    init {
        addFactory(factory)
    }

    override fun isApplicable(kind: RunnableProjectKind) = isTypeApplicable(kind)

    override fun tryCreateDefault(
            project: Project,
            lifetime: Lifetime,
            projects: List<RunnableProject>,
            runManager: RunManager
    ): Promise<List<RunnerAndConfigurationSettings>>? {

        val defaultSettingsList = mutableListOf<RunnerAndConfigurationSettings>()

        val applicableProjects = projects.filter {
            isApplicable(it.kind)
                    // Don't create "default" config if launchSettings.json is available for any project
                    && !(it.kind == RunnableProjectKind.LaunchSettings && LaunchSettingsJsonService.getLaunchSettingsFileForProject(it)?.exists() == true)
        }

        applicableProjects.forEach { applicableProject ->
            val defaultSettings =
                    runManager.createConfiguration(name = applicableProject.name, factory = factory)

            val configuration = defaultSettings.configuration as AzureFunctionsHostConfiguration
            configuration.parameters.projectFilePath = applicableProject.projectFilePath

            runManager.addConfiguration(defaultSettings)
            defaultSettingsList.add(defaultSettings)
        }

        return resolvedPromise(defaultSettingsList.toList())
    }
}
