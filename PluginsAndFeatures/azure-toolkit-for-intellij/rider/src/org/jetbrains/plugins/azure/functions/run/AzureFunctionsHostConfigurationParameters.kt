/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions.run

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.process.ProcessHandler
import com.intellij.ide.browsers.BrowserStarter
import com.intellij.ide.browsers.StartBrowserSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.platform.util.getComponent
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectKind
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.isUnloadedProject
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.configurations.dotNetExe.DotNetExeConfigurationParameters
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import org.jdom.Element
import org.jetbrains.plugins.azure.RiderAzureBundle.message
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfo
import org.jetbrains.plugins.azure.functions.coreTools.FunctionsCoreToolsInfoProvider
import java.io.File

open class AzureFunctionsHostConfigurationParameters(
        project: Project,
        exePath: String,
        programParameters: String,
        workingDirectory: String,
        envs: Map<String, String>,
        isPassParentEnvs: Boolean,
        useExternalConsole: Boolean,
        var projectFilePath: String,
        var trackProjectExePath: Boolean,
        var trackProjectArguments: Boolean,
        var trackProjectWorkingDirectory: Boolean,
        var projectKind: RunnableProjectKind,
        var projectTfm: String,
        var functionNames: String,
        var startBrowserParameters: DotNetStartBrowserParameters
) : DotNetExeConfigurationParameters(
        project,
        exePath,
        programParameters,
        workingDirectory,
        envs,
        isPassParentEnvs,
        useExternalConsole,
        false,
        false,
        null,
        ""
) {

    companion object {
        private const val PROJECT_PATH = "PROJECT_PATH"
        private const val PROJECT_EXE_PATH_TRACKING = "PROJECT_EXE_PATH_TRACKING"
        private const val PROJECT_ARGUMENTS_TRACKING = "PROJECT_ARGUMENTS_TRACKING"
        private const val PROJECT_WORKING_DIRECTORY_TRACKING = "PROJECT_WORKING_DIRECTORY_TRACKING"
        private const val PROJECT_KIND = "PROJECT_KIND"
        private const val PROJECT_TFM = "PROJECT_TFM"
        private const val FUNCTION_NAMES = "FUNCTION_NAMES"

        private val logger = Logger.getInstance(AzureFunctionsHostConfigurationParameters::class.java)
    }

    val isUnloadedProject: Boolean
        get() = project.getComponent<ProjectModelViewHost>().isUnloadedProject(projectFilePath)

    override fun toDotNetExecutable(): DotNetExecutable {
        val runnableProject = tryGetRunnableProject()
                ?: throw CantRunException("Project is not specified")

        val projectOutput = tryGetProjectOutput(runnableProject)

        val coreToolsInfo: FunctionsCoreToolsInfo? = FunctionsCoreToolsInfoProvider.retrieve()
                ?: throw CantRunException("Can't run Azure Functions host - path to core tools has not been configured.")

        val effectiveWorkingDirectory = if (trackProjectWorkingDirectory && projectOutput != null) {
            projectOutput.workingDirectory
        } else {
            workingDirectory
        }

        val effectiveArguments =
                if (trackProjectArguments &&
                        programParameters.isEmpty() &&
                        projectOutput != null &&
                        projectOutput.defaultArguments.isNotEmpty()
                ) {
                    ParametersListUtil.join(projectOutput.defaultArguments)
                } else {
                    programParameters
                }

        return DotNetExecutable(
                exePath = coreToolsInfo!!.coreToolsExecutable,
                projectTfm = projectOutput?.tfm ?: projectTfm,
                workingDirectory = effectiveWorkingDirectory,
                programParameterString = effectiveArguments,
                useMonoRuntime = useMonoRuntime,
                useExternalConsole = useExternalConsole,
                environmentVariables = envs,
                isPassParentEnvs = isPassParentEnvs,
                onBeforeProcessStarted = startBrowserAction,
                assemblyToDebug = coreToolsInfo.coreToolsExecutable,
                runtimeArguments = runtimeArguments,
                executeAsIs = true)
    }

    private val startBrowserAction: (RunProfile, ProcessHandler) -> Unit = { runProfile, processHandler ->
        if (startBrowserParameters.startAfterLaunch && runProfile is RunConfiguration) {
            val startBrowserSettings = StartBrowserSettings().apply {
                isSelected = startBrowserParameters.startAfterLaunch
                url = startBrowserParameters.url
                browser = startBrowserParameters.browser
                isStartJavaScriptDebugger = startBrowserParameters.withJavaScriptDebugger
            }

            BrowserStarter(runProfile, startBrowserSettings, processHandler).start()
        }
    }

    private fun tryGetRunnableProject(): RunnableProject? {
        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull
        if (runnableProjects.isNullOrEmpty())
            return null

        val applicableProjects = runnableProjects.filter {
            it.projectFilePath == projectFilePath && AzureFunctionsHostConfigurationType.isTypeApplicable(it.kind)
        }

        if (applicableProjects.size > 1) {
            logger.warn("Multiple applicable runnable projects detected for .NET Project configuration ($projectFilePath): ${applicableProjects.joinToString("; ")}")
        }

        val applicableProject = applicableProjects.firstOrNull()

        if (applicableProject != null)
            return AzureFunctionsRunnableProjectUtil.patchRunnableProjectOutputs(applicableProject)

        return null
    }

    private fun tryGetProjectOutput(runnableProject: RunnableProject): ProjectOutput? {
        return runnableProject.projectOutputs.singleOrNull { it.tfm == projectTfm }
                ?: runnableProject.projectOutputs.firstOrNull()
    }

    override fun validate(riderDotNetActiveRuntimeHost: RiderDotNetActiveRuntimeHost) {
        if (project.solution.isLoaded.valueOrNull != true)
            throw RuntimeConfigurationError(message("run_config.run_function_app.validation.solution_loading"))

        val runnableProject = tryGetRunnableProject()
                ?: throw RuntimeConfigurationError(message("run_config.run_function_app.validation.project_not_specified"))

        if (!runnableProject.problems.isNullOrEmpty())
            throw RuntimeConfigurationError(runnableProject.problems)

        if (!trackProjectExePath) {
            val exeFile = File(exePath)
            if (!exeFile.exists() || !exeFile.isFile)
                throw RuntimeConfigurationError(
                        message("run_config.run_function_app.validation.invalid_exe_path", if (exePath.isNotEmpty()) exePath else "<${message("common.empty")}>"))
        }

        if (!trackProjectWorkingDirectory) {
            val workingDirectoryFile = File(workingDirectory)
            if (!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory)
                throw RuntimeConfigurationError(
                        message("run_config.run_function_app.validation.invalid_working_directory", if (workingDirectory.isNotEmpty()) workingDirectory else "<${message("common.empty")}>"))
        }

        FunctionsCoreToolsInfoProvider.retrieve()
                ?: throw RuntimeConfigurationError(message("run_config.run_function_app.validation.missing_core_tools_path"))

        if (useMonoRuntime && riderDotNetActiveRuntimeHost.monoRuntime == null)
            throw RuntimeConfigurationError(message("run_config.run_function_app.validation.missing_mono_runtime"))
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        projectFilePath = JDOMExternalizerUtil.readField(element, PROJECT_PATH) ?: ""
        val trackProjectExePathString = JDOMExternalizerUtil.readField(element, PROJECT_EXE_PATH_TRACKING)
                ?: ""
        trackProjectExePath = trackProjectExePathString != "0"
        val trackProjectArgumentsString = JDOMExternalizerUtil.readField(element, PROJECT_ARGUMENTS_TRACKING)
                ?: ""
        trackProjectArguments = trackProjectArgumentsString != "0"
        val trackProjectWorkingDirectoryString = JDOMExternalizerUtil.readField(element, PROJECT_WORKING_DIRECTORY_TRACKING)
                ?: ""
        trackProjectWorkingDirectory = trackProjectWorkingDirectoryString != "0"
        projectKind = RunnableProjectKind.valueOf(JDOMExternalizerUtil.readField(element, PROJECT_KIND)
                ?: message("common.none_capitalized"))
        projectTfm = JDOMExternalizerUtil.readField(element, PROJECT_TFM) ?: ""
        functionNames = JDOMExternalizerUtil.readField(element, FUNCTION_NAMES) ?: ""
        startBrowserParameters = DotNetStartBrowserParameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JDOMExternalizerUtil.writeField(element, PROJECT_PATH, projectFilePath)
        JDOMExternalizerUtil.writeField(element, PROJECT_EXE_PATH_TRACKING, if (trackProjectExePath) "1" else "0")
        JDOMExternalizerUtil.writeField(element, PROJECT_ARGUMENTS_TRACKING, if (trackProjectArguments) "1" else "0")
        JDOMExternalizerUtil.writeField(element, PROJECT_WORKING_DIRECTORY_TRACKING, if (trackProjectWorkingDirectory) "1" else "0")
        JDOMExternalizerUtil.writeField(element, PROJECT_KIND, projectKind.toString())
        JDOMExternalizerUtil.writeField(element, PROJECT_TFM, projectTfm)
        JDOMExternalizerUtil.writeField(element, FUNCTION_NAMES, functionNames)
        startBrowserParameters.writeExternal(element)
    }
}
